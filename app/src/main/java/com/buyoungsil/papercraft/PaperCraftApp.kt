package com.buyoungsil.papercraft

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 앱 전체의 시작점.
 * @HiltAndroidApp을 붙이면 Hilt가 앱 전체에서 쓸 의존성 컨테이너를 여기서 만든다.
 * 싱글톤 객체(Repository 등)는 이 컨테이너가 살아있는 동안(앱 프로세스 동안) 하나씩 유지된다.
 */
@HiltAndroidApp
class PaperCraftApp : Application()