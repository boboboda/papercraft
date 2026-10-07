package com.buyoungsil.papercraft.feature.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buyoungsil.papercraft.core.ads.findActivity
import com.buyoungsil.papercraft.data.model.Picture
import com.buyoungsil.papercraft.feature.play.result.ResultView
import com.buyoungsil.papercraft.feature.play.sketch.PlacingBoard
import com.buyoungsil.papercraft.feature.play.sketch.SketchBoard
import com.buyoungsil.papercraft.feature.play.table.CutButton
import com.buyoungsil.papercraft.feature.play.table.CuttingTable
import com.buyoungsil.papercraft.feature.play.table.Joystick
import com.buyoungsil.papercraft.feature.play.table.RefPeek
import com.buyoungsil.papercraft.feature.play.table.TableInfoBar
import com.buyoungsil.papercraft.feature.play.table.TableLayout
import com.buyoungsil.papercraft.feature.play.table.TableLoop
import com.buyoungsil.papercraft.feature.play.table.rememberTableController
import com.buyoungsil.papercraft.game.logic.CutSession

/**
 * 플레이 화면. 단계(Stage)에 따라 부품을 갈아 끼운다.
 *   SKETCH → SketchStage / TABLE → TableStage / PLACING → PlacingBoard / RESULT → ResultView
 */
@Composable
fun PlayScreen(
    pictureId: String,                       // 실제 값은 ViewModel 이 SavedStateHandle 에서 읽음
    onBack: () -> Unit,
    onNext: (String) -> Unit = {},
    viewModel: PlayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardReady by viewModel.rewardedReady.collectAsStateWithLifecycle()   // ★
    val activity = LocalContext.current.findActivity()                          // ★ 광고는 Activity 위에 뜬다

    BackHandler {
        // ★ 결과 화면에서 뒤로 가기도 "메뉴로" 와 똑같이 전면 광고 판단을 거친다
        if (state.stage == Stage.RESULT) viewModel.afterResult(activity, onBack)
        else if (viewModel.back()) onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val picture = state.picture
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            state.error != null || picture == null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("그림을 불러오지 못했어요", style = MaterialTheme.typography.titleMedium)
                Text(state.error.orEmpty(), style = MaterialTheme.typography.bodySmall)
                Button(onClick = onBack) { Text("메뉴로") }
            }

            else -> when (state.stage) {
                Stage.SKETCH -> SketchStage(
                    picture = picture,
                    state = state,
                    onPieceTap = viewModel::selectPiece,
                    onBack = { if (viewModel.back()) onBack() },
                    onFinishEarly = viewModel::showResult
                )

                Stage.TABLE -> TableStage(picture, state, viewModel)

                Stage.PLACING -> state.placing?.let { placing ->
                    PlacingBoard(
                        picture = picture,
                        placed = state.placed,
                        placing = placing,
                        onMove = viewModel::moveBy,
                        onRotate = viewModel::rotateBy,
                        onFlip = viewModel::flip,
                        onRetrim = viewModel::retrim,
                        onConfirm = viewModel::confirmPlace
                    )
                }

                Stage.RESULT -> state.result?.let { result ->
                    ResultView(
                        picture = picture,
                        placed = state.placed,
                        result = result,
                        // ★ 세 버튼 모두 전면 광고 판단 → 광고가 닫힌 뒤 원래 동작
                        onRetry = { viewModel.afterResult(activity) { viewModel.restart() } },
                        onNext = { id -> viewModel.afterResult(activity) { onNext(id) } },
                        onMenu = { viewModel.afterResult(activity, onBack) }
                    )
                }
            }
        }

        PlayDialogs(
            dialog = state.dialog,
            canFinishEarly = state.placed.isNotEmpty(),
            rewardReady = rewardReady,                                           // ★
            onReward = { viewModel.watchRewardAd(activity) },                    // ★
            onFinishEarly = viewModel::showResult,
            onExit = onBack,
            onRemovePlaced = viewModel::removePlaced,
            onDismiss = viewModel::dismissDialog
        )
    }
}

// ───────────────────────── 밑그림 단계 ─────────────────────────

@Composable
private fun SketchStage(
    picture: Picture,
    state: PlayUiState,
    onPieceTap: (String) -> Unit,
    onBack: () -> Unit,
    onFinishEarly: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← 메뉴") }
            Text(
                text = picture.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = state.papersLeft?.let { "색종이 ${it}장" } ?: "연습 · 무제한",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        SketchBoard(
            picture = picture,
            placed = state.placed,
            onPieceTap = onPieceTap,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "자를 조각을 눌러요 · ${state.placed.size}/${picture.pieces.size}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (state.placed.isNotEmpty()) {
                TextButton(onClick = onFinishEarly) { Text("여기까지 완성") }
            }
        }
    }
}

// ───────────────────────── 자르기 단계 ─────────────────────────

@Composable
private fun TableStage(picture: Picture, state: PlayUiState, viewModel: PlayViewModel) {
    val table = state.table ?: return
    val piece = picture.pieces.firstOrNull { it.id == table.pieceId } ?: return
    val controller = rememberTableController()

    TableLoop(
        controller = controller,
        session = viewModel.session,
        sessionId = table.sessionId,
        onNeedPaper = viewModel::onNeedPaper,
        onCutting = viewModel::onCutting,
        onEvent = viewModel::onTableEvent
    )

    TableLayout(
        table = { m ->
            CuttingTable(
                controller = controller,
                paperColor = piece.color,
                modifier = m,
                onTap = { p ->
                    viewModel.onTableTap(p)
                    controller.refresh()
                }
            )
        },
        joystick = { size -> Joystick(controller.joystick, size = size) },
        cutButton = { size ->
            CutButton(
                state = controller.cutButton,
                size = size,
                enabled = controller.phase != CutSession.Phase.CHOOSING && state.dialog == null
            )
        },
        peek = { RefPeek(piece = piece, paperSize = table.paperSize) },
        info = {
            TableInfoBar(
                pieceName = piece.name,
                paperColor = piece.color,
                papersLeft = state.papersLeft,
                phase = controller.phase ?: CutSession.Phase.READY,
                canFinish = controller.canFinish,
                trimming = table.trimming,
                onBack = { viewModel.back() },
                onFinish = viewModel::finishCut
            )
        }
    )
}

// ───────────────────────── 창 ─────────────────────────

@Composable
private fun PlayDialogs(
    dialog: PlayDialog?,
    canFinishEarly: Boolean,
    rewardReady: Boolean,                                                        // ★
    onReward: () -> Unit,
    onFinishEarly: () -> Unit,
    onExit: () -> Unit,
    onRemovePlaced: (String) -> Unit,
    onDismiss: () -> Unit
) {
    when (dialog) {
        null -> Unit

        PlayDialog.OutOfPaper -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("색종이를 다 썼어요") },
            text = {
                Text(
                    if (rewardReady) "광고를 끝까지 보면 색종이 2장을 더 받을 수 있어요."
                    else "광고를 불러오는 중이에요. 잠시 후 다시 눌러 주세요."         // ★
                )
            },
            confirmButton = {
                Button(onClick = onReward, enabled = rewardReady) {                // ★ 준비 전엔 꺼 둠
                    Text(if (rewardReady) "광고 보고 +2장" else "광고 준비 중…")
                }
            },
            dismissButton = {
                if (canFinishEarly) TextButton(onClick = onFinishEarly) { Text("여기까지 완성") }
                else TextButton(onClick = onDismiss) { Text("닫기") }
            }
        )

        PlayDialog.ConfirmExit -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("그만둘까요?") },
            text = { Text("지금까지 붙인 조각은 저장되지 않아요.") },
            confirmButton = { TextButton(onClick = onExit) { Text("그만두기") } },
            dismissButton = { Button(onClick = onDismiss) { Text("계속하기") } }
        )

        is PlayDialog.PlacedPieceMenu -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("이미 붙인 조각이에요") },
            text = { Text("떼어 내고 새로 오릴까요? 색종이를 1장 더 써요.") },
            confirmButton = { Button(onClick = { onRemovePlaced(dialog.pieceId) }) { Text("다시 오리기") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("그대로 두기") } }
        )
    }
}