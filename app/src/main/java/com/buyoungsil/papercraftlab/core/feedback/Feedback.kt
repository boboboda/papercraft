package com.buyoungsil.papercraftlab.core.feedback

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진동 + 소리를 한 번에.
 * 화면/뷰모델은 Haptics, Sounds 를 따로 알 필요 없이 "무슨 일이 일어났는지"만 알려 준다.
 *
 *   feedback.split()      → 진동 "툭" + 찢는 소리
 *   feedback.cutting()    → 자르는 동안 매 프레임 (안에서 간격 조절)
 */
@Singleton
class Feedback @Inject constructor(
    private val haptics: Haptics,
    private val sounds: Sounds
) {
    /** 설정 화면용 스위치 (저장은 나중 단계) */
    var soundOn: Boolean
        get() = sounds.enabled
        set(value) { sounds.enabled = value }

    var vibrationOn: Boolean
        get() = haptics.enabled
        set(value) { haptics.enabled = value }

    // ───────────────────────── 자르기 테이블 ─────────────────────────

    /** 자르는 중 매 프레임 불러도 됨 (진동 70ms, 소리 180ms 간격으로 알아서 거름) */
    fun cutting() {
        haptics.cutTick()
        sounds.snip()
    }

    /** 가장자리에 닿아 종이가 갈라짐 */
    fun split() {
        haptics.split()
        sounds.play(Sounds.Sfx.SPLIT)
    }

    /** 고리를 닫아 안쪽을 오려냄 */
    fun closed() {
        haptics.closed()
        sounds.play(Sounds.Sfx.SPLIT, rate = 1.2f)    // 같은 소리를 조금 높게 → 구별되게
    }

    // ───────────────────────── 밑그림 / 붙이기 ─────────────────────────

    /** 조각 고르기, 남길 쪽 고르기, 일반 버튼 */
    fun click() {
        haptics.click()
        sounds.play(Sounds.Sfx.CLICK)
    }

    /** 조각을 밑그림에 붙임. 잘 붙였을수록(PERFECT) 소리가 살짝 높아짐 */
    fun paste(perfect: Boolean) {
        if (perfect) haptics.perfect() else haptics.click()
        sounds.play(Sounds.Sfx.PASTE, rate = if (perfect) 1.25f else 1f)
    }

    // ───────────────────────── 결과 ─────────────────────────

    /** 결과 화면. 별이 많을수록 더 화려하게 */
    fun result(stars: Int) {
        when {
            stars >= 3 -> {
                haptics.perfect()
                sounds.play(Sounds.Sfx.SUCCESS, rate = 1.1f)
            }
            stars >= 1 -> {
                haptics.click()
                sounds.play(Sounds.Sfx.SUCCESS)
            }
            else -> {
                haptics.click()
                sounds.play(Sounds.Sfx.SUCCESS, rate = 0.8f)   // 낮고 차분하게
            }
        }
    }
}