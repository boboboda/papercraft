package com.buyoungsil.papercraftlab.feature.play.sketch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import com.buyoungsil.papercraftlab.core.ui.component.toPath
import com.buyoungsil.papercraftlab.core.ui.theme.PaperColors
import com.buyoungsil.papercraftlab.data.model.Picture
import com.buyoungsil.papercraftlab.data.model.outline
import com.buyoungsil.papercraftlab.feature.play.PlacedPiece

/**
 * 밑그림 판 (300×300 좌표를 화면에 맞춰 그림).
 *
 *  - 칸 격자 (10 = 1칸)
 *  - 아직 안 붙인 조각: 점선 윤곽 + 옅은 색 (눌러서 고르기)
 *  - 붙인 조각: 실제로 자른 모양을 색종이 색으로
 *  - overlay: 붙이기 단계에서 움직이는 조각 등을 위에 덧그릴 때 (300 좌표계 그대로)
 *
 * @param onPieceTap 누른 곳에서 가장 위에 있는 조각 id (빈 곳이면 호출 안 함)
 */
@Composable
fun SketchBoard(
    picture: Picture,
    placed: Map<String, PlacedPiece>,
    modifier: Modifier = Modifier,
    highlightPieceId: String? = null,
    showOutlines: Boolean = true,
    onPieceTap: (String) -> Unit = {},
    overlay: (DrawScope.(k: Float) -> Unit)? = null
) {
    val tap by rememberUpdatedState(onPieceTap)
    val outlines = remember(picture) { picture.pieces.associate { it.id to it.shape.outline() } }

    Canvas(
        modifier = modifier.pointerInput(picture, placed) {
            detectTapGestures { pos ->
                val fit = fitBoard(size.toSize())
                val p = fit.toBoard(pos)
                // 위에 그려진 조각부터 검사 (조각 순서 = 층 순서 → 뒤에서부터)
                val hit = picture.pieces.asReversed().firstOrNull { piece ->
                    val shape = placed[piece.id]?.worldShape ?: outlines.getValue(piece.id)
                    pointInPoly(shape, p)
                }
                hit?.let { tap(it.id) }
            }
        }
    ) {
        val fit = fitBoard(size)
        drawRect(PaperColors.Wood)

        translate(fit.origin.x, fit.origin.y) {
            scale(fit.scale, pivot = Offset.Zero) {
                val k = fit.scale
                val px = 1f / k                         // 화면 1px 에 해당하는 두께

                // 바탕 + 격자
                drawRect(PaperColors.SketchPaper, size = Size(BOARD, BOARD))
                var g = 0f
                while (g <= BOARD) {
                    drawLine(PaperColors.SketchGrid, Offset(g, 0f), Offset(g, BOARD), px)
                    drawLine(PaperColors.SketchGrid, Offset(0f, g), Offset(BOARD, g), px)
                    g += CELL
                }

                // 조각들 (밑그림 순서 = 아래층부터)
                picture.pieces.forEach { piece ->
                    val done = placed[piece.id]
                    if (done != null) {
                        val path = done.worldShape.toPath()
                        drawPath(path, piece.color)
                        drawPath(path, Color.Black.copy(alpha = 0.12f), style = Stroke(px))
                    } else if (showOutlines) {
                        val path = outlines.getValue(piece.id).toPath()
                        val hl = piece.id == highlightPieceId
                        drawPath(path, piece.color.copy(alpha = if (hl) 0.35f else 0.15f))
                        drawPath(
                            path,
                            if (hl) PaperColors.CutLine else PaperColors.Guide,
                            style = Stroke(
                                width = if (hl) px * 2.5f else px * 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(px * 8, px * 6))
                            )
                        )
                    }
                }

                overlay?.invoke(this, k)
            }
        }
    }
}

// ───────────────────────── 좌표 ─────────────────────────

/** 300×300 판을 화면 가운데에 꽉 차게 */
data class BoardFit(val origin: Offset, val scale: Float) {
    fun toBoard(screen: Offset): Offset = (screen - origin) / scale
}

fun fitBoard(size: Size): BoardFit {
    val margin = size.minDimension * 0.03f
    val k = (size.minDimension - margin * 2) / BOARD
    return BoardFit(
        origin = Offset((size.width - BOARD * k) / 2f, (size.height - BOARD * k) / 2f),
        scale = k
    )
}

/** 반직선 교차법. (List.contains 멤버와 헷갈리지 않게 이름을 따로) */
fun pointInPoly(poly: List<Offset>, p: Offset): Boolean {
    var inside = false
    var j = poly.lastIndex
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[j]
        if ((a.y > p.y) != (b.y > p.y) &&
            p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
        ) inside = !inside
        j = i
    }
    return inside
}

private const val BOARD = 300f
private const val CELL = 10f