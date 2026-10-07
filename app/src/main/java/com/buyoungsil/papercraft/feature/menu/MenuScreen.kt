package com.buyoungsil.papercraft.feature.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buyoungsil.papercraft.core.ui.component.PictureThumbnail
import com.buyoungsil.papercraft.core.ui.theme.GradeColors
import com.buyoungsil.papercraft.core.ui.theme.PaperColors

/** 네비게이션에서 부르는 입구. ViewModel 을 꺼내 상태를 넘긴다. */
@Composable
fun MenuScreen(
    onPlay: (String) -> Unit,
    onGallery: () -> Unit,
    viewModel: MenuViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MenuContent(
        state = state,
        onPlay = onPlay,
        onGallery = onGallery,
        onRetry = viewModel::retry
    )
}

/** 상태만 받아서 그리는 부분 (ViewModel 모름 → 미리보기/테스트 쉬움) */
@Composable
fun MenuContent(
    state: MenuUiState,
    onPlay: (String) -> Unit,
    onGallery: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        when (state) {
            MenuUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is MenuUiState.Error -> ErrorView(state.message, onRetry, Modifier.align(Alignment.Center))
            is MenuUiState.Ready -> ReadyView(state, onPlay, onGallery)
        }
    }
}

// ───────────────────────── 준비 완료 ─────────────────────────

@Composable
private fun ReadyView(
    state: MenuUiState.Ready,
    onPlay: (String) -> Unit,
    onGallery: () -> Unit
) {
    // 폰 세로: 카드 작게 / 태블릿·가로: 카드 크게
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val cardWidth: Dp = if (wide) 168.dp else 128.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp),          // 태블릿에서 너무 퍼지지 않게
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Header(state.totalStars, state.maxStars, onGallery) }

        item {
            PracticeBanner(
                card = state.practice,
                onClick = { onPlay(state.practice.id) },
                thumbSize = if (wide) 96.dp else 72.dp
            )
        }

        items(state.chapters, key = { it.id }) { section ->
            ChapterRow(section, cardWidth, onPlay)
        }
    }
}

@Composable
private fun Header(totalStars: Int, maxStars: Int, onGallery: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "색종이 공작실",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "★ $totalStars / $maxStars",
                style = MaterialTheme.typography.titleSmall,
                color = GradeColors.Star
            )
        }
        OutlinedButton(onClick = onGallery) { Text("내 작품") }
    }
}

@Composable
private fun PracticeBanner(card: PictureCard, onClick: () -> Unit, thumbSize: Dp) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = PaperColors.Tip,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PictureThumbnail(
                picture = card.picture,
                modifier = Modifier
                    .size(thumbSize)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "연습하기",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PaperColors.TipText
                )
                Text(
                    text = "색종이 무제한 · 점수 없이 가위 감 익히기",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaperColors.TipText
                )
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = PaperColors.TipText)
        }
    }
}

@Composable
private fun ChapterRow(section: ChapterSection, cardWidth: Dp, onPlay: (String) -> Unit) {
    Column(Modifier.alpha(if (section.locked) 0.5f else 1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${section.id}장 ${section.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = section.theme,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${section.clearedCount}/${section.cards.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.cards, key = { it.id }) { card ->
                PictureCardView(
                    card = card,
                    width = cardWidth,
                    onClick = { if (!card.locked) onPlay(card.id) }
                )
            }
        }
    }
}

@Composable
private fun PictureCardView(card: PictureCard, width: Dp, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !card.locked, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(8.dp)) {
            Box {
                PictureThumbnail(
                    picture = card.picture,
                    silhouette = card.locked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                )
                if (card.locked) {
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stars(card.stars),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GradeColors.Star,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${card.pieceCount}조각",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun stars(n: Int): String = "★".repeat(n) + "☆".repeat(3 - n.coerceIn(0, 3))

// ───────────────────────── 에러 ─────────────────────────

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("그림을 불러오지 못했어요", style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) { Text("다시 시도") }
    }
}