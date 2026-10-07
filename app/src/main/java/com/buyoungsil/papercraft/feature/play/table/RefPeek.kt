package com.buyoungsil.papercraft.feature.play.table

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.buyoungsil.papercraft.core.ui.component.drawPiece
import com.buyoungsil.papercraft.core.ui.component.toPath
import com.buyoungsil.papercraft.core.ui.theme.PaperColors
import com.buyoungsil.papercraft.data.model.Piece
import com.buyoungsil.papercraft.data.model.outline
import com.buyoungsil.papercraft.data.model.bounds
import kotlin.math.roundToInt

/**
 * 자르는 동안 보는 참고 창.
 * - 목표 조각을 "종이와 같은 칸 격자" 위에 그려서, 몇 칸짜리인지 세면서 자를 수 있게 한다
 * - 종이 크기(paperSize)만큼의 영역을 그대로 보여 주므로 종이 대비 비율이 테이블과 같다
 * - 누르면 크게 / 다시 누르면 작게
 */
@Composable
fun RefPeek(
    piece: Piece,
    paperSize: Float,
    modifier: Modifier = Modifier,
    compactSize: Dp = 112.dp,
    expandedSize: Dp = 240.dp
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val boxSize by animateDpAsState(if (expanded) expandedSize else compactSize, label = "peek")

    // 조각을 종이 가운데로 옮길 거리 (조각 원래 좌표는 300×300 그림 기준)
    val layout = remember(piece, paperSize) {
        val b = piece.shape.bounds()
        PeekLayout(
            shift = Offset(paperSize / 2f - b.center.x, paperSize / 2f - b.center.y),
            cellsW = b.width / CELL,
            cellsH = b.height / CELL,
            outline = piece.shape.outline().toPath()
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.width(boxSize).padding(6.dp)) {
            Canvas(Modifier.size(boxSize - 12.dp)) {
                val k = size.minDimension / paperSize
                drawRect(PaperColors.SketchPaper)

                scale(k, pivot = Offset.Zero) {
                    // 칸 격자 (종이와 똑같이 10 = 1칸)
                    val w = 1f / k                  // 화면에서 1px 두께가 되도록
                    var x = 0f
                    while (x <= paperSize) {
                        drawLine(PaperColors.SketchGrid, Offset(x, 0f), Offset(x, paperSize), w * 1.5f)
                        x += CELL
                    }
                    var y = 0f
                    while (y <= paperSize) {
                        drawLine(PaperColors.SketchGrid, Offset(0f, y), Offset(paperSize, y), w * 1.5f)
                        y += CELL
                    }

                    translate(layout.shift.x, layout.shift.y) {
                        drawPiece(piece, cachedPath = null, color = piece.color)
                        drawPath(layout.outline, Color.Black.copy(alpha = 0.35f), style = Stroke(w * 1.5f))
                    }
                }
            }
            Text(
                text = piece.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "가로 ${fmt(layout.cellsW)} · 세로 ${fmt(layout.cellsH)}칸",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private class PeekLayout(
    val shift: Offset,
    val cellsW: Float,
    val cellsH: Float,
    val outline: androidx.compose.ui.graphics.Path
)

/** 0.5칸 단위로 반올림 (7.3 → "7.5", 8.0 → "8") */
private fun fmt(cells: Float): String {
    val half = (cells * 2).roundToInt() / 2f
    return if (half % 1f == 0f) half.toInt().toString() else half.toString()
}

private const val CELL = 10f