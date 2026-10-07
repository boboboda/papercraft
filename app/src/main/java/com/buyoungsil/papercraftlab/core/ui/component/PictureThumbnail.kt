package com.buyoungsil.papercraftlab.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import com.buyoungsil.papercraftlab.core.ui.theme.PaperColors
import com.buyoungsil.papercraftlab.data.model.Picture
import com.buyoungsil.papercraftlab.data.model.Piece
import com.buyoungsil.papercraftlab.data.model.Shape as PieceShape

/**
 * 그림 미리보기. 이미지 파일 없이 조각 모양을 그대로 축소해서 그린다.
 *
 * @param silhouette true 면 모든 조각을 한 색으로 (잠긴 그림 / 실루엣 퀴즈용)
 * @param pieceColorOverride 특정 조각만 다른 색으로 (예: 결과 화면에서 틀린 조각 강조)
 */
@Composable
fun PictureThumbnail(
    picture: Picture,
    modifier: Modifier = Modifier,
    background: Color = PaperColors.SketchPaper,
    silhouette: Boolean = false,
    silhouetteColor: Color = PaperColors.Guide.copy(alpha = 0.45f),
    pieceColorOverride: Map<String, Color> = emptyMap()
) {
    // 그림이 같으면 Path 를 다시 만들지 않는다 (300×300 좌표 그대로 보관)
    val paths = remember(picture) { picture.pieces.associate { it.id to it.toPathOrNull() } }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        drawRect(background)

        val s = size.minDimension / Picture.CANVAS
        scale(scale = s, pivot = Offset.Zero) {
            picture.pieces.forEach { piece ->
                val color = when {
                    silhouette -> silhouetteColor
                    else -> pieceColorOverride[piece.id] ?: piece.color
                }
                drawPiece(piece, paths[piece.id], color)
            }
        }
    }
}

/** 300×300 좌표계에서 조각 1개 그리기 (게임 화면에서도 재사용 가능) */
fun DrawScope.drawPiece(piece: Piece, cachedPath: Path?, color: Color) {
    when (val shape = piece.shape) {
        is PieceShape.Ellipse -> drawOval(
            color = color,
            topLeft = Offset(shape.center.x - shape.rx, shape.center.y - shape.ry),
            size = Size(shape.rx * 2, shape.ry * 2)
        )
        is PieceShape.Polygon -> {
            val path = cachedPath ?: shape.points.toPath()
            drawPath(path, color)
        }
    }
}

private fun Piece.toPathOrNull(): Path? =
    (shape as? PieceShape.Polygon)?.points?.toPath()

fun List<Offset>.toPath(): Path = Path().apply {
    if (isEmpty()) return@apply
    moveTo(first().x, first().y)
    for (i in 1 until size) lineTo(this@toPath[i].x, this@toPath[i].y)
    close()
}