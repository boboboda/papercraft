package com.buyoungsil.papercraftlab.feature.play.table

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 자르기 버튼 상태.
 * isPressed 는 게임 루프가 매 프레임 읽고, 버튼 그림도 이 값으로 바뀐다.
 * (눌렀다/뗐다는 자주 바뀌지 않으므로 Compose 상태로 둬도 부담 없음)
 */
@Stable
class CutButtonState {
    var isPressed by mutableStateOf(false)
        internal set
}

@Composable
fun rememberCutButtonState(): CutButtonState = remember { CutButtonState() }

/**
 * 오른쪽 자르기 버튼. 누르고 있는 동안만 가위가 나아간다.
 *
 * @param enabled false 면 (조각 선택 중 등) 눌러도 반응 없음
 */
@Composable
fun CutButton(
    state: CutButtonState,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    enabled: Boolean = true
) {
    // 비활성화되는 순간 눌림 상태도 풀어 준다 (누른 채로 선택 화면이 뜨는 경우)
    LaunchedEffect(enabled) {
        if (!enabled) state.isPressed = false
    }

    val idle = MaterialTheme.colorScheme.secondary
    val pressed = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
    val ring = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.35f)
    val onColor = MaterialTheme.colorScheme.onSecondary

    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    state.isPressed = true
                    // 손을 떼거나, 버튼 밖으로 나가서 취소되거나, 다른 제스처에 뺏길 때까지
                    waitForUpOrCancellation()
                    state.isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            // 누르면 살짝 작아져서 눌린 느낌
            val bodyR = if (state.isPressed) r * 0.92f else r
            drawCircle(Color.Black.copy(alpha = 0.12f), radius = r, center = center.copy(y = center.y + 3.dp.toPx()))
            drawCircle(if (state.isPressed) pressed else idle, radius = bodyR)
            drawCircle(ring, radius = bodyR * 0.82f, style = Stroke(2.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✂", color = onColor, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = if (state.isPressed) "자르는 중" else "꾹 누르기",
                color = onColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}