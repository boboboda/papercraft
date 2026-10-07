package com.buyoungsil.papercraftlab.feature.play.table

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.buyoungsil.papercraftlab.core.ui.theme.PaperColors
import com.buyoungsil.papercraftlab.game.logic.CutSession

/**
 * 테이블 위 정보 줄.
 *  - 지금 자르는 조각 이름, 남은 색종이, 상황별 안내 문구, "오려내기 완료" 버튼
 *  - 폭이 넓으면 한 줄, 좁으면(가로 모드 왼쪽 칸) 세로로 쌓는다
 *
 * @param papersLeft null = 무제한(연습)
 * @param trimming   "더 다듬기" 중이면 true (새 종이를 쓰지 않음)
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TableInfoBar(
    pieceName: String,
    paperColor: Color,
    papersLeft: Int?,
    phase: CutSession.Phase,
    canFinish: Boolean,
    trimming: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hint = hintFor(phase, trimming)

    BoxWithConstraints(modifier) {
        val narrow = maxWidth < 360.dp

        if (narrow) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BackButton(onBack)
                PieceTitle(pieceName, paperColor)
                PaperCount(papersLeft, trimming)
                HintText(hint)
                FinishButton(canFinish, onFinish, Modifier.fillMaxWidth())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onBack)
                    Spacer(Modifier.width(4.dp))
                    PieceTitle(pieceName, paperColor, Modifier.weight(1f))
                    PaperCount(papersLeft, trimming)
                    Spacer(Modifier.width(10.dp))
                    FinishButton(canFinish, onFinish)
                }
                HintText(hint)
            }
        }
    }
}

private fun hintFor(phase: CutSession.Phase, trimming: Boolean): String = when (phase) {
    CutSession.Phase.READY ->
        if (trimming) "조각 가장자리부터 다시 다듬어요 · 다 되면 완료"
        else "종이 가장자리를 눌러 시작점을 옮길 수 있어요"
    CutSession.Phase.CUTTING -> "버튼을 누르고 있는 동안 잘려요 · 왼쪽으로 방향 조절"
    CutSession.Phase.CHOOSING -> "남길 쪽을 눌러 주세요"
    CutSession.Phase.CLOSED -> "구멍이 뚫렸어요!"
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    TextButton(onClick = onBack) { Text("← 밑그림") }
}

@Composable
private fun PieceTitle(name: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PaperCount(papersLeft: Int?, trimming: Boolean) {
    val text = when {
        trimming -> "다듬는 중"
        papersLeft == null -> "연습 · 무제한"
        else -> "색종이 ${papersLeft}장"
    }
    val warn = !trimming && papersLeft != null && papersLeft <= 1
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (warn) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun HintText(hint: String) {
    Surface(
        color = PaperColors.Tip,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = PaperColors.TipText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FinishButton(enabled: Boolean, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onFinish, enabled = enabled, modifier = modifier) {
        Text("오려내기 완료")
    }
}