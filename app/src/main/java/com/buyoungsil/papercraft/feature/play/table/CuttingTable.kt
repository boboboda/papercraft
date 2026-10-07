package com.buyoungsil.papercraft.feature.play.table

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.buyoungsil.papercraft.core.ui.component.toPath
import com.buyoungsil.papercraft.core.ui.theme.PaperColors
import com.buyoungsil.papercraft.game.logic.CutSession
import com.buyoungsil.papercraft.game.logic.centroid
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 자르기 테이블 그림.
 *  나무 바탕 → 종이 그림자 → 종이 + 칸 격자 → 가위 자국 → 예상 자르기 선 → 가위
 *  조각 선택 중(CHOOSING)에는 갈라진 두 조각을 살짝 벌려서 보여 준다.
 *
 * @param onTap 종이 좌표로 바꾼 탭 위치.
 *              READY 면 시작점 옮기기, CHOOSING 이면 남길 조각 고르기 → 판단은 ViewModel 이
 */
@Composable
fun CuttingTable(
    controller: TableController,
    paperColor: Color,
    modifier: Modifier = Modifier,
    onTap: (Offset) -> Unit = {}
) {
    val tap by rememberUpdatedState(onTap)

    Canvas(
        modifier = modifier.pointerInput(controller) {
            detectTapGestures { pos ->
                val s = controller.session ?: return@detectTapGestures
                val fit = fit(size.toSize(), s.paperSize)
                tap(fit.toPaper(pos))
            }
        }
    ) {
        controller.frame                       // 이 값이 바뀌면 다시 그린다 (그리기 단계에서만 읽음)
        drawRect(PaperColors.Wood)

        val s = controller.session ?: return@Canvas
        val fit = fit(size, s.paperSize)

        translate(fit.origin.x, fit.origin.y) {
            val k = fit.scale

            if (s.phase == CutSession.Phase.CHOOSING) {
                drawChoices(s, k, paperColor)
            } else {
                drawSheet(s.sheet, k, paperColor)
                drawCutPath(s.path, k)
                drawGuideToEdge(s, k)
                drawScissors(
                    pos = s.position * k,
                    angle = s.angle,
                    target = controller.joystick.direction,
                    cutting = controller.cutButton.isPressed
                )
            }
        }
    }
}

// ───────────────────────── 좌표 맞추기 ─────────────────────────

/** 종이(0..paper)를 화면 가운데에 여백 두고 맞춘 결과 */
private data class Fit(val origin: Offset, val scale: Float) {
    fun toPaper(screen: Offset): Offset = (screen - origin) / scale
}

private fun fit(size: Size, paper: Float): Fit {
    val margin = size.minDimension * 0.06f
    val scale = (size.minDimension - margin * 2) / paper
    val origin = Offset(
        (size.width - paper * scale) / 2f,
        (size.height - paper * scale) / 2f
    )
    return Fit(origin, scale)
}

private fun List<Offset>.scaled(k: Float): List<Offset> = map { it * k }

// ───────────────────────── 종이 ─────────────────────────

private fun DrawScope.drawSheet(sheet: List<Offset>, k: Float, color: Color, shift: Offset = Offset.Zero) {
    if (sheet.size < 3) return
    val path = sheet.scaled(k).toPath()

    translate(shift.x, shift.y) {
        // 그림자
        translate(0f, 3.dp.toPx()) { drawPath(path, Color.Black.copy(alpha = 0.18f)) }
        // 종이
        drawPath(path, color)
        // 칸 격자 (10 = 1칸). 종이 모양 안에서만 보이게 잘라서 그린다
        clipPath(path) { drawGrid(sheet, k) }
        // 테두리
        drawPath(path, Color.Black.copy(alpha = 0.15f), style = Stroke(1.dp.toPx()))
    }
}

private fun DrawScope.drawGrid(sheet: List<Offset>, k: Float) {
    val maxX = sheet.maxOf { it.x }
    val maxY = sheet.maxOf { it.y }
    val minX = sheet.minOf { it.x }
    val minY = sheet.minOf { it.y }
    val w = 1.dp.toPx()

    var x = (minX / CELL).toInt() * CELL
    while (x <= maxX) {
        drawLine(PaperColors.PaperGrid, Offset(x * k, minY * k), Offset(x * k, maxY * k), w)
        x += CELL
    }
    var y = (minY / CELL).toInt() * CELL
    while (y <= maxY) {
        drawLine(PaperColors.PaperGrid, Offset(minX * k, y * k), Offset(maxX * k, y * k), w)
        y += CELL
    }
}

/** 갈라진 조각들을 중심에서 바깥으로 살짝 벌려서 그린다 */
private fun DrawScope.drawChoices(s: CutSession, k: Float, color: Color) {
    val choices: List<List<Offset>> = s.choices?.toList() ?: emptyList()
    val all = choices.flatten()
    if (all.isEmpty()) return
    val center = all.centroid()
    val gap = 6.dp.toPx()

    choices.forEach { poly ->
        val d = poly.centroid() - center
        val len = d.getDistance()
        val shift = if (len > 0.001f) d * (gap / len) else Offset.Zero
        drawSheet(poly, k, color, shift)
        // "눌러서 고르세요" 느낌의 점선 테두리
        translate(shift.x, shift.y) {
            drawPath(
                poly.scaled(k).toPath(),
                Color.White.copy(alpha = 0.9f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            )
        }
    }
}

// ───────────────────────── 가위 자국 ─────────────────────────

private fun DrawScope.drawCutPath(path: List<Offset>, k: Float) {
    if (path.size < 2) return
    val p = Path().apply {
        moveTo(path[0].x * k, path[0].y * k)
        for (i in 1 until path.size) lineTo(path[i].x * k, path[i].y * k)
    }
    drawPath(
        p, PaperColors.CutLine,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// ───────────────────────── 예상 자르기 선 ─────────────────────────

/**
 * 지금 방향 그대로 끝까지 가면 종이 어디에 닿는지 점선으로.
 * (출발 직후 바깥을 향해 막혀 있으면 그리지 않음)
 */
private fun DrawScope.drawGuideToEdge(s: CutSession, k: Float) {
    val dir = Offset(cos(s.angle), sin(s.angle))
    val end = rayExit(s.sheet, s.position, dir) ?: return
    val from = s.position * k
    val to = end * k
    val dash = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx()))

    // 흰 테두리 + 진한 점선 → 어떤 색종이 위에서도 잘 보이게
    drawLine(Color.White.copy(alpha = 0.7f), from, to, strokeWidth = 4.dp.toPx(), pathEffect = dash, cap = StrokeCap.Round)
    drawLine(PaperColors.Guide, from, to, strokeWidth = 2.dp.toPx(), pathEffect = dash, cap = StrokeCap.Round)
    // 닿는 지점 표시
    drawCircle(Color.White, radius = 5.dp.toPx(), center = to)
    drawCircle(PaperColors.CutLine, radius = 3.5.dp.toPx(), center = to)
}

/**
 * from 에서 dir 방향으로 쏜 반직선이 다각형 테두리와 처음 만나는 점.
 * 출발점이 테두리 위에 있어도(시작 직후) 자기 자신은 건너뛴다.
 */
private fun rayExit(poly: List<Offset>, from: Offset, dir: Offset): Offset? {
    var bestT = Float.MAX_VALUE
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        val e = b - a
        val denom = cross(dir, e)
        if (abs(denom) < 1e-6f) continue                   // 평행
        val w = a - from
        val t = cross(w, e) / denom                        // 반직선 위 거리
        val u = cross(w, dir) / denom                      // 선분 위 위치 (0..1)
        if (t > 0.05f && u in 0f..1f && t < bestT) bestT = t
    }
    return if (bestT == Float.MAX_VALUE) null else from + dir * bestT
}

private fun cross(p: Offset, q: Offset): Float = p.x * q.y - p.y * q.x

// ───────────────────────── 가위 ─────────────────────────

private fun DrawScope.drawScissors(pos: Offset, angle: Float, target: Float?, cutting: Boolean) {
    // 크기 (날 36dp, 고리 11dp)
    val blade = 36.dp.toPx()
    val ring = 11.dp.toPx()

    // 조이스틱이 가리키는 목표 방향 (가위는 이쪽으로 천천히 돈다)
    if (target != null) {
        val t = Offset(cos(target), sin(target))
        drawCircle(Color.White, radius = 6.dp.toPx(), center = pos + t * (blade * 1.5f))
        drawCircle(PaperColors.Tip, radius = 4.5.dp.toPx(), center = pos + t * (blade * 1.5f))
    }

    rotateRad(angle, pivot = pos) {
        // 날 (앞쪽 삼각형 두 개를 살짝 벌림)
        val open = if (cutting) 8.dp.toPx() else 3.dp.toPx()
        val bladeLight = Color(0xFFB4BCC6)
        val bladeDark = Color(0xFF8C949E)
        val edge = Color(0xFF4A5058)
        val upper = Path().apply {
            moveTo(pos.x + blade, pos.y)
            lineTo(pos.x, pos.y - open)
            lineTo(pos.x, pos.y)
            close()
        }
        val lower = Path().apply {
            moveTo(pos.x + blade, pos.y)
            lineTo(pos.x, pos.y + open)
            lineTo(pos.x, pos.y)
            close()
        }
        drawPath(upper, bladeLight)
        drawPath(lower, bladeDark)
        drawPath(upper, edge, style = Stroke(1.dp.toPx()))
        drawPath(lower, edge, style = Stroke(1.dp.toPx()))

        // ★ 손잡이 고리 두 개 (뒤쪽) — 흰 테두리를 먼저 깔고 그 위에 빨강
        //    → 빨간 종이(지붕 등) 위에서도 손잡이가 보인다
        val handle = Color(0xFFD8452F)
        val c1 = Offset(pos.x - ring * 1.15f, pos.y - ring * 0.95f)
        val c2 = Offset(pos.x - ring * 1.15f, pos.y + ring * 0.95f)
        val outline = Stroke(8.dp.toPx())
        val stroke = Stroke(4.5.dp.toPx())
        drawCircle(Color.White, radius = ring, center = c1, style = outline)
        drawCircle(Color.White, radius = ring, center = c2, style = outline)
        drawCircle(handle, radius = ring, center = c1, style = stroke)
        drawCircle(handle, radius = ring, center = c2, style = stroke)

        // 축
        drawCircle(Color.White, radius = 4.5.dp.toPx(), center = pos)
        drawCircle(Color(0xFF555B63), radius = 3.dp.toPx(), center = pos)
    }
}

private const val CELL = 10f