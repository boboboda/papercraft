package com.buyoungsil.papercraft.feature.play

import androidx.compose.ui.geometry.Offset
import com.buyoungsil.papercraft.data.model.Picture
import com.buyoungsil.papercraft.game.logic.PieceResult
import com.buyoungsil.papercraft.game.logic.PieceScore
import com.buyoungsil.papercraft.game.logic.Poly
import com.buyoungsil.papercraft.game.logic.ResultSummary
import kotlin.math.cos
import kotlin.math.sin

/**
 * 플레이 화면 전체 상태.
 *
 *   SKETCH ──조각 고르기──▶ TABLE ──오려내기 완료──▶ PLACING ──붙이기──▶ SKETCH ... ──다 붙이면──▶ RESULT
 *                              ▲                         │
 *                              └────── 더 다듬기 ─────────┘
 *
 * 매 프레임 바뀌는 값(가위 위치 등)은 여기 없다. 그건 CutSession + TableController 몫.
 * 여기는 "단계가 바뀔 때"만 바뀌는 값들.
 */
data class PlayUiState(
    val loading: Boolean = true,
    val error: String? = null,

    val picture: Picture? = null,
    val stage: Stage = Stage.SKETCH,

    /** 붙인 조각들 (조각 id → 붙인 결과) */
    val placed: Map<String, PlacedPiece> = emptyMap(),

    /** 남은 색종이. null = 무제한(연습) */
    val papersLeft: Int? = null,

    val table: TableUi? = null,
    val placing: PlacingUi? = null,
    val result: ResultUi? = null,

    val dialog: PlayDialog? = null
) {
    val isPractice: Boolean get() = papersLeft == null

    /** 아직 안 붙인 조각 id 들 (밑그림 순서대로) */
    val remainingPieceIds: List<String>
        get() = picture?.pieces?.map { it.id }?.filter { it !in placed } ?: emptyList()

    val allPlaced: Boolean get() = picture != null && remainingPieceIds.isEmpty()
}

enum class Stage { SKETCH, TABLE, PLACING, RESULT }

/** 자르기 테이블 단계 정보 */
data class TableUi(
    val pieceId: String,
    val paperSize: Float,
    /** "더 다듬기" 중이면 true → 종이 소모 없음 */
    val trimming: Boolean,
    /** 새 CutSession 이 만들어질 때마다 +1 → 화면이 새 세션으로 갈아 끼우는 신호 */
    val sessionId: Int
)

/** 붙이기 단계 정보 */
data class PlacingUi(
    val pieceId: String,
    /** 자른 모양. 무게중심이 (0,0)이 되게 옮겨 둔 좌표 */
    val cut: Poly,
    val transform: PieceTransform
)

/** 붙인 조각 1개 */
data class PlacedPiece(
    val pieceId: String,
    val cut: Poly,                    // 무게중심 (0,0) 기준
    val transform: PieceTransform,
    val score: PieceScore
) {
    /** 밑그림(300×300) 좌표로 옮긴 실제 모양 */
    val worldShape: Poly get() = transform.apply(cut)
}

/**
 * 붙이기 변환: 뒤집기 → 회전 → 이동 순서.
 * @param position 밑그림 좌표에서 조각 무게중심이 놓일 곳
 * @param rotation 도(degree), 시계 방향 +
 */
data class PieceTransform(
    val position: Offset,
    val rotation: Float = 0f,
    val flipped: Boolean = false
) {
    fun apply(local: Poly): Poly {
        val rad = Math.toRadians(rotation.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return local.map { p ->
            val x = if (flipped) -p.x else p.x
            val y = p.y
            Offset(
                x * c - y * s + position.x,
                x * s + y * c + position.y
            )
        }
    }
}

/** 결과 화면 */
data class ResultUi(
    val summary: ResultSummary,       // 제목 〈귀가 없는 고양이〉, 별, 평균 칸
    val pieces: List<PieceResult>,    // 조각별 점수 (밑그림 순서)
    val nextPictureId: String?        // 다음 그림 (없으면 null)
)

/** 화면 위에 뜨는 창 */
sealed interface PlayDialog {
    /** 색종이를 다 썼어요 → 광고 보고 +2장 / 그만두기 */
    data object OutOfPaper : PlayDialog
    /** 뒤로 가기 → 지금까지 한 것 버리고 나갈까요? */
    data object ConfirmExit : PlayDialog
    /** 이미 붙인 조각을 눌렀을 때 → 다시 붙이기(위치만) */
    data class PlacedPieceMenu(val pieceId: String) : PlayDialog
}