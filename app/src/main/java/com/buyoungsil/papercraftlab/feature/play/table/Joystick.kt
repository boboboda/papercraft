package com.buyoungsil.papercraftlab.feature.play.table

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 조이스틱 상태.
 *
 * direction: 게임 루프(매 프레임)가 읽는 값. 화면을 다시 그릴 필요가 없으니 일반 var.
 *            라디안, 화면 좌표 기준 (0 = 오른쪽, -π/2 = 위). 손을 떼거나 중앙 근처면 null.
 * knob:      손잡이 그림 위치. 그림이 바뀌어야 하니 Compose 상태.
 */
@Stable
class JoystickState {
    var direction: Float? = null
        internal set

    /** 중앙 기준 -1..1 정규화 위치 */
    var knob by mutableStateOf(Offset.Zero)
        internal set

    val isActive: Boolean get() = direction != null

    internal fun reset() {
        direction = null
        knob = Offset.Zero
    }
}

@Composable
fun rememberJoystickState(): JoystickState = remember { JoystickState() }

/**
 * 왼쪽 방향 조이스틱.
 * 원 안 어디를 눌러도 시작되고, 손가락이 원 밖으로 나가도 방향은 계속 유지된다.
 */
@Composable
fun Joystick(
    state: JoystickState,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    deadZone: Float = 0.18f            // 반지름 대비, 이 안쪽이면 방향 없음
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val ringColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val tickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val knobColor = MaterialTheme.colorScheme.primary
    val knobActive = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)

    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(deadZone) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val radius = minOf(this.size.width, this.size.height) / 2f

                    fun update(pos: Offset) {
                        val d = pos - center
                        val len = d.getDistance()
                        // 손잡이는 원 안에서만 움직인다
                        val clamped = if (len > radius) d * (radius / len) else d
                        state.knob = clamped / radius
                        state.direction =
                            if (len < radius * deadZone) null
                            else atan2(d.y, d.x)
                    }

                    update(down.position)
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        update(change.position)
                        change.consume()
                    }
                    state.reset()
                }
            }
    ) {
        val r = this.size.minDimension / 2f
        val c = center

        // 바탕 원
        drawCircle(baseColor, radius = r)
        drawCircle(ringColor, radius = r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))

        // 8방향 눈금 (방향 감 잡기용)
        for (i in 0 until 8) {
            val a = i * (Math.PI / 4).toFloat()
            val dir = Offset(cos(a), sin(a))
            drawLine(
                color = tickColor,
                start = c + dir * (r * 0.78f),
                end = c + dir * (r * 0.92f),
                strokeWidth = if (i % 2 == 0) 3.dp.toPx() else 1.5.dp.toPx()
            )
        }

        // 데드존 표시
        drawCircle(ringColor, radius = r * deadZone, style = Stroke(1.dp.toPx()))

        // 손잡이
        val knobR = r * 0.36f
        val travel = r - knobR
        val knobCenter = c + state.knob * travel
        drawCircle(if (state.isActive) knobActive else knobColor, radius = knobR, center = knobCenter)
    }
}