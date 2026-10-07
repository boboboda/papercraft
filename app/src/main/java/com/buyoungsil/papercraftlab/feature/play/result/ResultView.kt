package com.buyoungsil.papercraftlab.feature.play.result

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buyoungsil.papercraftlab.core.ui.theme.GradeColors
import com.buyoungsil.papercraftlab.data.model.Picture
import com.buyoungsil.papercraftlab.feature.play.PlacedPiece
import com.buyoungsil.papercraftlab.feature.play.ResultUi
import com.buyoungsil.papercraftlab.feature.play.sketch.SketchBoard
import com.buyoungsil.papercraftlab.game.logic.Grade
import com.buyoungsil.papercraftlab.game.logic.PieceResult
import java.util.Locale

/**
 * 결과 화면.
 *  〈귀가 없는 고양이〉 + ★★☆ + 완성 그림 + 조각별 등급 + 버튼
 *  넓은 화면이면 그림 | 내용 좌우 배치
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ResultView(
    picture: Picture,
    placed: Map<String, PlacedPiece>,
    result: ResultUi,
    onRetry: () -> Unit,
    onNext: (String) -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth > maxHeight && maxWidth >= 600.dp

        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FinishedPicture(picture, placed, Modifier.weight(1f).fillMaxSize())
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Details(picture, result, onRetry, onNext, onMenu)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FinishedPicture(
                    picture, placed,
                    Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Spacer(Modifier.height(12.dp))
                Details(picture, result, onRetry, onNext, onMenu)
            }
        }
    }
}

@Composable
private fun FinishedPicture(picture: Picture, placed: Map<String, PlacedPiece>, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp))) {
        // 점선 윤곽 없이 붙인 조각만 → 안 붙인 조각은 정말로 "없는" 그림이 된다
        SketchBoard(
            picture = picture,
            placed = placed,
            showOutlines = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun Details(
    picture: Picture,
    result: ResultUi,
    onRetry: () -> Unit,
    onNext: (String) -> Unit,
    onMenu: () -> Unit
) {
    val s = result.summary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "〈${s.title}〉",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (!picture.practice) {
            Text(
                text = "★".repeat(s.stars) + "☆".repeat((3 - s.stars).coerceAtLeast(0)),
                style = MaterialTheme.typography.displaySmall,
                color = GradeColors.Star
            )
        }
        Text(
            text = "평균 오차 ${fmt1(s.avgCells)}칸",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        // 조각별 등급
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                result.pieces.forEach { PieceRow(it) }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 버튼
        val next = result.nextPictureId
        if (next != null) {
            Button(onClick = { onNext(next) }, modifier = Modifier.fillMaxWidth()) { Text("다음 그림 →") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("다시 하기") }
            TextButton(onClick = onMenu, modifier = Modifier.weight(1f)) { Text("메뉴로") }
        }
    }
}

@Composable
private fun PieceRow(r: PieceResult) {
    val score = r.score
    val (label, color) = when {
        score == null -> "없음" to GradeColors.Bad
        else -> score.grade.label to gradeColor(score.grade)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(r.piece.color)
        )
        Spacer(Modifier.width(8.dp))
        Text(r.piece.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (score != null) {
            Text(
                text = "${fmt1(score.cells)}칸",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
        }
        Surface(color = color, shape = RoundedCornerShape(6.dp)) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun gradeColor(g: Grade): Color = when (g) {
    Grade.PERFECT -> GradeColors.Perfect
    Grade.GOOD -> GradeColors.Good
    Grade.MEH -> GradeColors.Meh
    Grade.BAD -> GradeColors.Bad
}

private fun fmt1(v: Number): String = String.format(Locale.US, "%.1f", v.toFloat())