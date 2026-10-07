package com.buyoungsil.papercraft.data.repository

import android.util.Log
import com.buyoungsil.papercraft.data.model.PictureProgress
import com.buyoungsil.papercraft.data.model.Progress
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진행 저장소 (Firestore).
 *
 * 문서 구조 — 사용자당 문서 1개:
 *   users/{uid}
 *     completedCount: 12
 *     removeAds: false
 *     pictures: {
 *       house: { bestStars: 3, bestAvgCells: 0.42, bestTitle: "완벽한 집", plays: 4, updatedAt: 1759... }
 *       cat:   { ... }
 *     }
 *
 * - 오프라인 캐시 켜 둠 → 인터넷 없어도 읽기/쓰기 즉시, 연결되면 알아서 서버와 맞춤
 * - 쓰기는 기다리지 않는다 (TaskAwait.kt 주석 참고)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ProgressRepository @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
    }

    /** 앱이 살아 있는 동안 도는 범위 (저장소가 싱글턴이므로 앱 수명과 같음) */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 진행 상황. 로그인 전/실패면 빈 Progress.
     * 다른 기기에서 바뀌어도(나중에 계정 연결 시) 실시간으로 따라온다.
     */
    val progress: StateFlow<Progress> = authRepository.uid
        .flatMapLatest { uid -> if (uid == null) flowOf(Progress()) else observe(uid) }
        .stateIn(scope, SharingStarted.Eagerly, Progress())

    init {
        // 앱 시작하자마자 로그인 시도 (실패해도 앱은 계속 동작, 다음 저장 때 다시 시도)
        scope.launch { trySignIn() }
    }

    // ───────────────────────── 읽기 ─────────────────────────

    private fun observe(uid: String) = callbackFlow {
        val registration = userDoc(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "진행 불러오기 실패", error)
                return@addSnapshotListener
            }
            trySend(parse(snap?.data))
        }
        awaitClose { registration.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parse(data: Map<String, Any?>?): Progress {
        if (data == null) return Progress()
        val pics = (data["pictures"] as? Map<String, Map<String, Any?>>).orEmpty()
        return Progress(
            pictures = pics.mapValues { (id, m) ->
                PictureProgress(
                    pictureId = id,
                    bestStars = (m["bestStars"] as? Number)?.toInt() ?: 0,
                    bestAvgCells = (m["bestAvgCells"] as? Number)?.toFloat(),
                    bestTitle = m["bestTitle"] as? String,
                    plays = (m["plays"] as? Number)?.toInt() ?: 0,
                    updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0L
                )
            },
            completedCount = (data["completedCount"] as? Number)?.toInt() ?: 0,
            removeAds = data["removeAds"] as? Boolean ?: false
        )
    }

    // ───────────────────────── 쓰기 ─────────────────────────

    /**
     * 그림 1장 완성 결과 저장. (연습 그림은 저장하지 않음)
     * @return 이번 결과까지 포함한 완성 횟수 (전면 광고 판단용)
     */
    suspend fun recordResult(
        pictureId: String,
        stars: Int,
        avgCells: Float,
        title: String
    ): Int {
        val current = progress.value
        val newCount = current.completedCount + 1
        val uid = authRepository.uid.value ?: trySignIn() ?: run {
            Log.w(TAG, "로그인 안 됨 → 이번 결과는 저장 못 함")
            return newCount
        }

        val merged = current.of(pictureId).merge(stars, avgCells, title, System.currentTimeMillis())
        val data = mapOf(
            "completedCount" to FieldValue.increment(1),
            "pictures" to mapOf(
                pictureId to mapOf(
                    "bestStars" to merged.bestStars,
                    "bestAvgCells" to merged.bestAvgCells,
                    "bestTitle" to merged.bestTitle,
                    "plays" to merged.plays,
                    "updatedAt" to merged.updatedAt
                )
            )
        )
        // merge = 안 적은 필드/다른 그림 기록은 건드리지 않음. 기다리지 않음(오프라인 대비)
        userDoc(uid).set(data, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "결과 저장 실패: $pictureId", it) }

        return newCount
    }

    /** "광고 제거" 구매 반영 (결제 단계에서 사용) */
    fun setRemoveAds(value: Boolean) {
        val uid = authRepository.uid.value ?: return
        userDoc(uid).set(mapOf("removeAds" to value), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "광고 제거 저장 실패", it) }
    }

    // ───────────────────────── 내부 ─────────────────────────

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private suspend fun trySignIn(): String? = try {
        authRepository.ensureSignedIn()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "익명 로그인 실패 (인터넷 확인)", e)
        null
    }

    private companion object {
        const val TAG = "ProgressRepository"
    }
}