// 프로젝트 전체에서 쓸 플러그인을 선언만 해둠 (apply false).
// 실제 적용은 app/build.gradle.kts에서 함.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
}