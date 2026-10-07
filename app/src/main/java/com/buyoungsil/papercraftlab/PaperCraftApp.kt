package com.buyoungsil.papercraftlab

import android.app.Application
import com.buyoungsil.papercraftlab.core.analytics.Analytics
import dagger.hilt.android.HiltAndroidApp

/**
 * 앱 전체의 시작점.
 * @HiltAndroidApp을 붙이면 Hilt가 앱 전체에서 쓸 의존성 컨테이너를 여기서 만든다.
 * 싱글톤 객체(Repository 등)는 이 컨테이너가 살아있는 동안(앱 프로세스 동안) 하나씩 유지된다.
 */
@HiltAndroidApp
class PaperCraftApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 앱 분석 (홈페이지 /admin/analytics). 주소·키가 비어 있으면 아무것도 하지 않는다.
        Analytics.init(
            application = this,
            baseUrl = BuildConfig.ANALYTICS_URL,
            ingestKey = BuildConfig.ANALYTICS_KEY,
            versionName = BuildConfig.VERSION_NAME,
            isDebug = BuildConfig.DEBUG
        )
    }
}