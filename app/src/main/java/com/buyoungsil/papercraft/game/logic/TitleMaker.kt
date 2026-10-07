package com.buyoungsil.papercraft.game.logic

import com.buyoungsil.papercraft.data.model.Piece
import kotlin.math.abs
import kotlin.math.min

/**
 * 조각 하나의 최종 결과.
 * @param score null이면 색종이가 없어서 못 붙인 조각
 */
data class PieceResult(
    val piece: Piece,
    val score: PieceScore?,
)

/**
 * 그림 한 장의 완성 결과.
 * @param title 완성작 제목 ("귀가 작은 고양이")
 * @param stars 별 0~3개
 * @param avgCells 평균 오차 (칸)
 */
data class ResultSummary(
    val title: String,
    val stars: Int,
    val avgCells: Float,
)

object TitleMaker {

    /** "고양이" 같은 이름에 붙이는 수식어 제거용 ("왼쪽 귀" → "귀") */
    private val PREFIX = Regex("^(왼쪽|오른쪽|큰|작은) ")

    /**
     * 모든 조각 결과로 제목과 별점을 만든다.
     * 제목은 가장 많이 틀린 조각 하나를 기준으로 붙인다.
     *
     * @param noun 그림 이름 ("고양이")
     */
    fun summarize(noun: String, results: List<PieceResult>): ResultSummary {
        require(results.isNotEmpty()) { "조각이 하나도 없음" }

        val avgCells = results
            .map { min(errorOf(it), Scoring.MISSING_ERROR) }
            .average()
            .toFloat() / Scoring.CELL

        val worst = results.maxBy { errorOf(it) }

        return ResultSummary(
            title = "${adjective(worst)} $noun",
            stars = Scoring.stars(avgCells),
            avgCells = avgCells,
        )
    }

    private fun errorOf(r: PieceResult): Float = r.score?.error ?: Scoring.MISSING_ERROR

    /**
     * 제목 앞에 붙는 말을 고른다.
     *  - 못 붙인 조각         → "귀가 없는"
     *  - 전부 0.5칸 이하      → "완벽한"
     *  - 크기는 비슷(±12%)한데 모양이 틀림 → "몸통이 울퉁불퉁한"
     *  - 크게 자름            → 조각의 adjBig  ("귀가 쫑긋한")
     *  - 작게 자름            → 조각의 adjSmall ("귀가 작은")
     */
    private fun adjective(r: PieceResult): String {
        val score = r.score
        val base = r.piece.name.replace(PREFIX, "")
        val josa = subjectJosa(base)
        return when {
            score == null -> "$base$josa 없는"
            score.error <= 5f -> "완벽한"
            abs(score.areaRatio - 1f) < 0.12f -> "$base$josa 울퉁불퉁한"
            score.areaRatio > 1f -> r.piece.adjBig
            else -> r.piece.adjSmall
        }
    }

    /**
     * 주격 조사 "이/가" 고르기.
     * 한글 음절은 (글자 코드 - '가') % 28 이 받침 번호. 0이면 받침 없음 → "가"
     * 예: 머리 → 가, 몸통 → 이, 눈 → 이
     */
    fun subjectJosa(word: String): String {
        val last = word.lastOrNull() ?: return "가"
        if (last !in '가'..'힣') return "이"   // 한글이 아니면 기본값
        return if ((last - '가') % 28 != 0) "이" else "가"
    }
}