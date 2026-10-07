import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// ★ 구글 공식 테스트 광고 단위 (아무 앱 ID 와 같이 써도 됨, 눌러도 안전)
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"

// ★ 내 실제 광고 단위 (AdMob 앱: com.buyoungsil.papercraftlab) — release 에서만 사용
val releaseRewardedId = "ca-app-pub-8596470561558049/8093150926"
val releaseInterstitialId = "ca-app-pub-8596470561558049/7228635121"

android {
    namespace = "com.buyoungsil.papercraftlab"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.buyoungsil.papercraftlab"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // ★ 안드로이드 스튜디오 실행 버튼 = debug → 항상 테스트 광고
        debug {
            buildConfigField("String", "AD_UNIT_REWARDED", "\"$testRewardedId\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$testInterstitialId\"")
        }
        // ★ 스토어 업로드용 = release → 내 실제 광고 단위
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "AD_UNIT_REWARDED", "\"$releaseRewardedId\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$releaseInterstitialId\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true          // ★ BuildConfig.AD_UNIT_... 를 코드에서 쓰려면 필요
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowsize)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // JSON
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)

    // Firebase (BoM 이 버전을 맞춰 줌 → 개별 버전 안 씀)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)

    // ★ 광고 (AdMob)
    implementation(libs.play.services.ads)

    // Test
    testImplementation(libs.junit)
}