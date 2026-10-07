package com.buyoungsil.papercraftlab.game.logic

import com.buyoungsil.papercraftlab.data.model.Picture
import com.buyoungsil.papercraftlab.data.model.Shape
import com.buyoungsil.papercraftlab.data.model.bounds
import kotlin.math.ceil
import kotlin.math.max

/**
 * 색종이 장수, 종이 크기, 광고 주기처럼 "숫자로 정한 규칙"을 한곳에 모음.
 * 밸런스를 바꿀 때는 이 파일만 고치면 된다.
 */
object PaperRules {

    /** 조각 수보다 더 주는 여유 장수 */
    const val EXTRA_PAPERS = 2

    /** 리워드 광고 한 번에 받는 장수 */
    const val REWARD_PAPERS = 2

    /** 그림 몇 장 완성마다 전면 광고 (연습 판 제외) */
    const val INTERSTITIAL_EVERY = 3

    /** 색종이 크기 = 조각 최대 길이 × 이 배율 (자를 여유 공간) */
    private const val PAPER_MARGIN = 1.45f

    /** 색종이 최소 크기 (눈·코처럼 작은 조각도 너무 작지 않게) */
    private const val PAPER_MIN = 50f

    /**
     * 그림 하나에 주는 색종이 장수.
     * @return null = 무제한 (연습 판)
     */
    fun papersFor(picture: Picture): Int? =
        if (picture.practice) null else picture.pieces.size + EXTRA_PAPERS

    /**
     * 조각에 맞는 색종이 한 변 길이.
     * 조각 최대 길이 × 1.45를 모눈 한 칸(10) 단위로 올림, 최소 50.
     * 모눈 칸 단위로 맞춰야 색종이 위 모눈이 가장자리에서 깔끔하게 끝난다.
     */
    fun paperSizeFor(shape: Shape): Float {
        val b = shape.bounds()
        val raw = max(b.width, b.height) * PAPER_MARGIN
        return max(PAPER_MIN, ceil(raw / Scoring.CELL) * Scoring.CELL)
    }

    /** 이번 완성으로 전면 광고를 띄울지 (완성 횟수는 연습 판 제외하고 셈) */
    fun shouldShowInterstitial(completedCount: Int): Boolean =
        completedCount > 0 && completedCount % INTERSTITIAL_EVERY == 0
}