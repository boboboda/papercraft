package com.buyoungsil.papercraft.core.feedback

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진동 피드백 모음. 앱 전체에 하나 (@Singleton).
 *
 *   cutTick()   자르는 동안 잔잔하게 "사각사각" (자주 불러도 알아서 간격 조절)
 *   split()     종이가 갈라질 때 "툭"
 *   closed()    구멍이 뚫렸을 때 "톡톡"
 *   click()     버튼, 조각 고르기
 *   perfect()   PERFECT / 별 3개 축하 "따다닥"
 *
 * minSdk 26 이라 VibrationEffect 는 항상 쓸 수 있고,
 * API 29+ 에서는 기기 제조사가 다듬어 둔 기본 효과(EFFECT_TICK 등)를 쓴다 → 더 자연스러움.
 * 권한: AndroidManifest 의 VIBRATE (8번에서 이미 넣음)
 */
@Singleton
class Haptics @Inject constructor(
    @ApplicationContext context: Context
) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /** 설정 화면에서 끌 수 있게 (설정 저장은 나중 단계) */
    var enabled: Boolean = true

    private val available: Boolean get() = enabled && vibrator?.hasVibrator() == true

    private var lastTickAt = 0L

    /** 자르는 중 매 프레임 불러도 됨. CUT_TICK_MS 간격으로만 실제 진동 */
    fun cutTick() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTickAt < CUT_TICK_MS) return
        lastTickAt = now
        predefinedOr(VibrationEffect.EFFECT_TICK) { oneShot(8, 40) }
    }

    fun split() = predefinedOr(VibrationEffect.EFFECT_HEAVY_CLICK) { oneShot(30, 200) }

    fun closed() = vibrate(
        VibrationEffect.createWaveform(
            longArrayOf(0, 20, 60, 20),          // 쉼, 진동, 쉼, 진동
            intArrayOf(0, 160, 0, 160),
            -1                                    // 반복 안 함
        )
    )

    fun click() = predefinedOr(VibrationEffect.EFFECT_CLICK) { oneShot(15, 120) }

    fun perfect() = vibrate(
        VibrationEffect.createWaveform(
            longArrayOf(0, 25, 50, 25, 50, 60),
            intArrayOf(0, 140, 0, 180, 0, 255),
            -1
        )
    )

    // ───────────────────────── 내부 ─────────────────────────

    private fun predefinedOr(effectId: Int, fallback: () -> VibrationEffect) {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(effectId)
        } else {
            fallback()
        }
        vibrate(effect)
    }

    private fun oneShot(ms: Long, amplitude: Int): VibrationEffect =
        VibrationEffect.createOneShot(ms, amplitude.coerceIn(1, 255))

    private fun vibrate(effect: VibrationEffect) {
        if (!available) return
        try {
            vibrator?.vibrate(effect)
        } catch (_: Exception) {
            // 일부 기기에서 진폭 미지원 등으로 실패해도 게임은 계속
        }
    }

    private companion object {
        /** 사각사각 간격. 너무 짧으면 계속 떨리는 느낌, 너무 길면 끊김 */
        const val CUT_TICK_MS = 70L
    }
}