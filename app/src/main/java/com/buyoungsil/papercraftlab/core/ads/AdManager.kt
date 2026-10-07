package com.buyoungsil.papercraftlab.core.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.buyoungsil.papercraftlab.BuildConfig
import com.buyoungsil.papercraftlab.core.analytics.Analytics
import com.buyoungsil.papercraftlab.game.logic.PaperRules
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 광고 창구. 앱 전체에 하나.
 *
 *  - 보상형: "광고 보고 색종이 +2" → 끝까지 봤을 때만 보상
 *  - 전면  : 그림 완성 N번마다 (연습 제외, 광고 제거 구매자 제외)
 *  - 둘 다 미리 불러 둔다 → 버튼 누르면 바로 뜸. 보여 준 뒤 다음 것을 다시 불러 둔다.
 *
 * 광고 단위 ID 는 BuildConfig 에서 (debug = 항상 테스트 광고, 104번 참고)
 * Google 광고 SDK 의 load/show 는 메인 스레드에서 불러야 한다 → scope 를 Main 으로.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var initialized = false

    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var loadingRewarded = false
    private var loadingInterstitial = false
    private var rewardedRetry = 0
    private var interstitialRetry = 0

    private val _rewardedReady = MutableStateFlow(false)
    /** 보상형 광고가 준비됐는지 → "광고 보고 +2장" 버튼 활성화에 사용 */
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    /** 앱 시작 시 한 번 (MainActivity 에서) */
    fun initialize() {
        if (initialized) return
        initialized = true

        if (CHILD_DIRECTED) {
            // 대상 연령에 13세 미만이 포함되면 true 로 → 맞춤 광고 끔, 어린이용 등급만
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .build()
            )
        }

        // 초기화는 백그라운드에서 (메인 스레드를 막지 않게)
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {}
        }
        loadRewarded()
        loadInterstitial()
    }

    // ───────────────────────── 보상형 ─────────────────────────

    private fun loadRewarded() {
        if (rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(
            context,
            BuildConfig.AD_UNIT_REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    loadingRewarded = false
                    rewardedRetry = 0
                    _rewardedReady.value = true
                    ad.onPaidEventListener = OnPaidEventListener { v ->
                        Analytics.Ads.paid(FORMAT_REWARDED, v.valueMicros, v.currencyCode)
                    }
                    Analytics.Ads.loaded(FORMAT_REWARDED)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingRewarded = false
                    Log.w(TAG, "보상형 불러오기 실패: ${error.message}")
                    Analytics.Ads.loadFailed(FORMAT_REWARDED, error.code)
                    retryLater(rewardedRetry++) { loadRewarded() }
                }
            }
        )
    }

    /**
     * 보상형 광고 보여 주기.
     * @param onResult 광고가 닫힌 뒤 호출. earned = 끝까지 봐서 보상 받을 자격이 있는지
     */
    fun showRewarded(activity: Activity, onResult: (earned: Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null) {
            onResult(false)
            loadRewarded()
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdImpression() = Analytics.Ads.impression(FORMAT_REWARDED)
            override fun onAdClicked() = Analytics.Ads.click(FORMAT_REWARDED)

            override fun onAdDismissedFullScreenContent() {
                clearRewarded()
                onResult(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "보상형 표시 실패: ${error.message}")
                clearRewarded()
                onResult(false)
            }
        }
        ad.show(activity) {                        // 끝까지 보면 불림
            earned = true
            Analytics.Ads.rewardEarned(FORMAT_REWARDED)
        }
    }

    private fun clearRewarded() {
        rewarded = null
        _rewardedReady.value = false
        loadRewarded()                            // 다음 것 미리
    }

    // ───────────────────────── 전면 ─────────────────────────

    private fun loadInterstitial() {
        if (interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            context,
            BuildConfig.AD_UNIT_INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                    interstitialRetry = 0
                    ad.onPaidEventListener = OnPaidEventListener { v ->
                        Analytics.Ads.paid(FORMAT_INTERSTITIAL, v.valueMicros, v.currencyCode)
                    }
                    Analytics.Ads.loaded(FORMAT_INTERSTITIAL)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                    Log.w(TAG, "전면 불러오기 실패: ${error.message}")
                    Analytics.Ads.loadFailed(FORMAT_INTERSTITIAL, error.code)
                    retryLater(interstitialRetry++) { loadInterstitial() }
                }
            }
        )
    }

    /**
     * 그림 완성 후 결과 화면에서 다음 행동(다음 그림/메뉴/다시 하기)을 누를 때 호출.
     * 보여 줄 차례가 아니거나 광고가 없으면 바로 onDone.
     *
     * @param completedCount 이번까지 포함한 완성 횟수 (연습 제외)
     * @param removeAds "광고 제거" 구매자면 true
     */
    fun maybeShowInterstitial(
        activity: Activity,
        completedCount: Int,
        removeAds: Boolean,
        onDone: () -> Unit
    ) {
        val ad = interstitial
        if (removeAds || !PaperRules.shouldShowInterstitial(completedCount) || ad == null) {
            if (ad == null) loadInterstitial()
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdImpression() = Analytics.Ads.impression(FORMAT_INTERSTITIAL)
            override fun onAdClicked() = Analytics.Ads.click(FORMAT_INTERSTITIAL)

            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "전면 표시 실패: ${error.message}")
                interstitial = null
                loadInterstitial()
                onDone()
            }
        }
        ad.show(activity)
    }

    // ───────────────────────── 내부 ─────────────────────────

    /** 실패하면 30초, 60초, 120초 … 최대 5분 간격으로 다시 시도 */
    private fun retryLater(attempt: Int, block: () -> Unit) {
        val waitMs = (30_000L shl attempt.coerceAtMost(4)).coerceAtMost(300_000L)
        scope.launch {
            delay(waitMs)
            block()
        }
    }

    private companion object {
        const val TAG = "AdManager"
        const val FORMAT_REWARDED = "rewarded"
        const val FORMAT_INTERSTITIAL = "interstitial"

        /**
         * 대상 연령에 13세 미만이 포함되면 true 로 바꿀 것 (구글 가족 정책).
         * 13세 이상만 대상이면 false 그대로.
         */
        const val CHILD_DIRECTED = false
    }
}

/** Compose 의 LocalContext 에서 Activity 꺼내기 (광고 표시에 Activity 필요) */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}