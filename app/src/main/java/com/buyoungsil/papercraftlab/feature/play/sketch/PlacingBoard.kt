package com.buyoungsil.papercraftlab.feature.play.sketch

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.buyoungsil.papercraftlab.core.ui.component.toPath
import com.buyoungsil.papercraftlab.core.ui.theme.PaperColors
import com.buyoungsil.papercraftlab.data.model.Picture
import com.buyoungsil.papercraftlab.feature.play.PlacedPiece
import com.buyoungsil.papercraftlab.feature.play.PlacingUi

/**
 * 붙이기 화면.
 *  - 한 손가락 드래그: 옮기기
 *  - 두 손가락 비틀기: 돌리기
 *  - 버튼: 15° 돌리기 / 뒤집기 / 더 다듬기 / 붙이기
 */
@Composable
fun PlacingBoard(
    picture: Picture,
    placed: Map<String, PlacedPiece>,
    placing: PlacingUi,
    onMove: (androidx.compose.ui.geometry.Offset) -> Unit,   // 300 좌표계 이동량
    onRotate: (Float) -> Unit,                                // 도(degree)
    onFlip: () -> Unit,
    onRetrim: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val move by rememberUpdatedState(onMove)
    val rotate by rememberUpdatedState(onRotate)
    val color = picture.pieces.first { it.id == placing.pieceId }.color
    val pieceName = picture.pieces.first { it.id == placing.pieceId }.name

    Column(modifier.fillMaxSize()) {
        Text(
            text = "‘$pieceName’ 을(를) 점선 자리에 맞춰 놓아요",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            SketchBoard(
                picture = picture,
                placed = placed,
                highlightPieceId = placing.pieceId,
                modifier = Modifier.fillMaxSize(),
                overlay = { k ->
                    val shape = placing.transform.apply(placing.cut)
                    val path = shape.toPath()
                    // 살짝 들려 있는 느낌: 그림자 + 약간 투명
                    drawPath(path, Color.Black.copy(alpha = 0.18f), alpha = 1f)
                    drawPath(path, color.copy(alpha = 0.9f))
                    drawPath(path, PaperColors.CutLine, style = Stroke(2f / k))
                }
            )

            // 제스처 전용 투명 층 (밑그림 판의 탭 검사와 겹치지 않게 위에 덮음)
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, rotation ->
                            val k = fitBoard(size.toSize()).scale
                            if (pan.x != 0f || pan.y != 0f) move(pan / k)
                            if (rotation != 0f) rotate(rotation)
                        }
                    }
            )
        }

        // 조작 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { onRotate(-15f) }, modifier = Modifier.weight(1f)) { Text("⟲ 15°") }
                FilledTonalButton(onClick = { onRotate(15f) }, modifier = Modifier.weight(1f)) { Text("⟳ 15°") }
                FilledTonalButton(onClick = onFlip, modifier = Modifier.weight(1f)) { Text("⇋ 뒤집기") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onRetrim, modifier = Modifier.weight(1f)) { Text("✂ 더 다듬기") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("붙이기") }
            }
        }
    }
}