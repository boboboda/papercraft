package com.buyoungsil.papercraft.game.logic

import com.buyoungsil.papercraft.data.model.Shape
import com.buyoungsil.papercraft.data.model.area
import com.buyoungsil.papercraft.data.model.outline

/** 조각 하나의 판정 등급 */
enum class Grade(val label: String) {
    PERFECT("완벽!"),
    GOOD("좋아요"),
    MEH("아쉬워요"),
    BAD("삐뚤!"),
}

/**
 * 조각 하나의 판정 결과.
 *
 * @param error 테두리 평균 오차 (밑그림 단위. 10 = 모눈 한 칸)
 * @param areaRatio 오린 넓이 / 목표 넓이. 1보다 크면 크게, 작으면 작게 자른 것 (제목 만들 때 사용)
 */
data class PieceScore(
    val error: Float,
    val areaRatio: Float,
) {
    /** 화면에 보여줄 오차 (칸 단위, 예: 0.8칸) */
    val cells: Float get() = error / Scoring.CELL
    val grade: Grade get() = Scoring.grade(error)
}

object Scoring {
    /** 모눈 한 칸 크기 */
    const val CELL = 10f

    /** 색종이가 없어서 못 붙인 조각은 4칸 오차로 계산 */
    const val MISSING_ERROR = 40f

    /** 테두리를 몇 단위 간격으로 촘촘하게 찍어서 비교할지 */
    private const val SAMPLE_STEP = 1.5f

    /**
     * 목표 모양과 오려 붙인 조각을 비교.
     *
     * 방법: 양쪽 테두리에 점을 촘촘히 찍고,
     *  ① 목표 테두리의 각 점 → 오린 테두리까지 최단거리 평균
     *  ② 오린 테두리의 각 점 → 목표 테두리까지 최단거리 평균
     *  두 값의 평균 = "테두리가 평균 몇 단위 어긋났나"
     *
     * 양방향으로 재는 이유: 한쪽만 재면 목표보다 훨씬 크게 잘라도
     * 목표 테두리 근처에 점이 있기만 하면 점수가 좋게 나오는 구멍이 생김.
     *
     * 겹침 비율(IoU)과 달리 조각 크기와 상관없이 기준이 같다.
     * (가는 돛대나 작은 눈도 큰 몸통과 같은 "칸" 기준으로 판정)
     */
    fun score(target: Shape, cut: Poly): PieceScore {
        val targetOutline = target.outline()
        val targetPoints = targetOutline.resample(SAMPLE_STEP)
        val cutPoints = cut.resample(SAMPLE_STEP)

        val targetToCut = targetPoints.map { cut.distanceTo(it) }.average()
        val cutToTarget = cutPoints.map { targetOutline.distanceTo(it) }.average()

        return PieceScore(
            error = ((targetToCut + cutToTarget) / 2).toFloat(),
            areaRatio = cut.area() / target.area(),
        )
    }

    /** 오차 → 등급 */
    fun grade(error: Float): Grade = when {
        error <= 5f -> Grade.PERFECT    // 0.5칸 이하
        error <= 10f -> Grade.GOOD      // 1칸 이하
        error <= 20f -> Grade.MEH       // 2칸 이하
        else -> Grade.BAD
    }

    /** 그림 전체 평균 오차(칸) → 별 개수 */
    fun stars(avgCells: Float): Int = when {
        avgCells <= 0.6f -> 3
        avgCells <= 1.0f -> 2
        avgCells <= 1.6f -> 1
        else -> 0
    }
}