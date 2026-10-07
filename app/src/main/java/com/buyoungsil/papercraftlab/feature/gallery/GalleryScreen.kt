package com.buyoungsil.papercraftlab.feature.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buyoungsil.papercraftlab.core.ui.component.toPath
import com.buyoungsil.papercraftlab.core.ui.theme.GradeColors
import com.buyoungsil.papercraftlab.core.ui.theme.PaperColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 크게 보기 중이면 뒤로 = 닫기
    val opened = (state as? GalleryUiState.Ready)?.opened
    BackHandler(enabled = opened != null) { viewModel.close() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← 메뉴") }
            Text(
                text = "내 작품" + ((state as? GalleryUiState.Ready)?.let { " ${it.totalCount}" } ?: ""),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        when (val s = state) {
            GalleryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is GalleryUiState.Ready -> {
                if (s.isEmpty) EmptyView()
                else ReadyView(
                    state = s,
                    onFilter = viewModel::selectFilter,
                    onOpen = viewModel::open
                )
            }
        }
    }

    opened?.let { card ->
        OpenedDialog(
            card = card,
            onClose = viewModel::close,
            onDelete = { viewModel.delete(card.id) }
        )
    }
}

// ───────────────────────── 목록 ─────────────────────────

@Composable
private fun ReadyView(
    state: GalleryUiState.Ready,
    onFilter: (String?) -> Unit,
    onOpen: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedPictureId == null,
                    onClick = { onFilter(null) },
                    label = { Text("전체") }
                )
            }
            items(state.filters, key = { it.pictureId }) { f ->
                FilterChip(
                    selected = state.selectedPictureId == f.pictureId,
                    onClick = { onFilter(f.pictureId) },
                    label = { Text("${f.name} ${f.count}") }
                )
            }
        }

        // 칸 폭 최소 150dp → 폰 세로 2열, 폰 가로 4열, 태블릿 4~6열 자동
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.cards, key = { it.id }) { card ->
                ArtworkCardView(card, onClick = { onOpen(card.id) })
            }
        }
    }
}

@Composable
private fun ArtworkCardView(card: ArtworkCard, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(8.dp)) {
            ArtworkCanvas(
                card = card,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "〈${card.title}〉",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stars(card.stars),
                    color = GradeColors.Star,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (!card.complete) {
                    Text(
                        text = card.pieceSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✂", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text("아직 작품이 없어요", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "그림을 완성하면 오린 모양 그대로 여기에 모여요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ───────────────────────── 크게 보기 ─────────────────────────

@Composable
private fun OpenedDialog(card: ArtworkCard, onClose: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArtworkCanvas(
                    card = card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                )
                Text(
                    text = "〈${card.title}〉",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(stars(card.stars), color = GradeColors.Star, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${card.picture.name} · ${card.pieceSummary} · ${dateText(card.artwork.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                        Text("지우기")
                    }
                    Button(onClick = onClose, modifier = Modifier.weight(1f)) { Text("닫기") }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("작품을 지울까요?") },
            text = { Text("지운 작품은 되돌릴 수 없어요. 별 기록은 그대로 남아요.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("지우기") }
            },
            dismissButton = { Button(onClick = { confirmDelete = false }) { Text("취소") } }
        )
    }
}

// ───────────────────────── 작품 그리기 ─────────────────────────

/** 저장된 오린 모양을 원래 그림의 색으로 그린다 (밑그림 300×300 좌표) */
@Composable
fun ArtworkCanvas(card: ArtworkCard, modifier: Modifier = Modifier) {
    val colors = remember(card.picture) { card.picture.pieces.associate { it.id to it.color } }
    val paths = remember(card.artwork) { card.artwork.pieces.map { it.pieceId to it.points.toPath() } }

    Canvas(modifier) {
        drawRect(PaperColors.SketchPaper)
        val k = size.minDimension / BOARD
        val ox = (size.width - BOARD * k) / 2f
        val oy = (size.height - BOARD * k) / 2f
        drawContext.transform.translate(ox, oy)
        scale(k, pivot = Offset.Zero) {
            var g = 0f
            while (g <= BOARD) {
                drawLine(PaperColors.SketchGrid, Offset(g, 0f), Offset(g, BOARD), 1f / k)
                drawLine(PaperColors.SketchGrid, Offset(0f, g), Offset(BOARD, g), 1f / k)
                g += 10f
            }
            paths.forEach { (id, path) ->
                drawPath(path, colors[id] ?: Color.Gray)
                drawPath(path, Color.Black.copy(alpha = 0.12f), style = Stroke(1f / k))
            }
        }
        drawContext.transform.translate(-ox, -oy)
    }
}

private fun stars(n: Int) = "★".repeat(n.coerceIn(0, 3)) + "☆".repeat(3 - n.coerceIn(0, 3))

private fun dateText(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(millis))

private const val BOARD = 300f