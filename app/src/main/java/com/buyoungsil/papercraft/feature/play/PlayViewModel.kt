package com.buyoungsil.papercraft.feature.play

import android.app.Activity
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buyoungsil.papercraft.core.ads.AdManager                           // ★
import com.buyoungsil.papercraft.core.feedback.Feedback
import com.buyoungsil.papercraft.data.model.Artwork
import com.buyoungsil.papercraft.data.model.ArtworkPiece
import com.buyoungsil.papercraft.data.model.Piece
import com.buyoungsil.papercraft.data.model.area
import com.buyoungsil.papercraft.data.model.centroid
import com.buyoungsil.papercraft.data.repository.ArtworkRepository
import com.buyoungsil.papercraft.data.repository.PictureRepository
import com.buyoungsil.papercraft.data.repository.ProgressRepository
import com.buyoungsil.papercraft.game.logic.CutSession
import com.buyoungsil.papercraft.game.logic.Grade
import com.buyoungsil.papercraft.game.logic.PaperRules
import com.buyoungsil.papercraft.game.logic.PieceResult
import com.buyoungsil.papercraft.game.logic.Poly
import com.buyoungsil.papercraft.game.logic.Scoring
import com.buyoungsil.papercraft.game.logic.TitleMaker
import com.buyoungsil.papercraft.game.logic.centroid
import com.buyoungsil.papercraft.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 그림 1장을 처음부터 끝까지 진행하는 뷰모델.
 * 화면 회전에도 살아남으므로, 자르던 종이(CutSession)도 여기서 들고 있는다.
 *
 * 광고는 Activity 위에 떠야 하므로 화면이 Activity 를 "빌려 주며" 호출한다.
 * (뷰모델이 Activity 를 저장하면 메모리 누수 → 함수 인자로만 받고 보관하지 않음)
 */
@HiltViewModel
class PlayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pictureRepository: PictureRepository,
    private val progressRepository: ProgressRepository,
    private val artworkRepository: ArtworkRepository,
    private val feedback: Feedback,
    private val adManager: AdManager                                            // ★
) : ViewModel() {

    private val pictureId: String = checkNotNull(savedStateHandle[Routes.ARG_PICTURE_ID])

    private val _uiState = MutableStateFlow(PlayUiState())
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()

    /** ★ 보상형 광고 준비 여부 → "광고 보고 +2장" 버튼 활성화 */
    val rewardedReady: StateFlow<Boolean> = adManager.rewardedReady

    /** 지금 자르는 종이. 매 프레임 바뀌므로 StateFlow 에 넣지 않고 화면이 직접 빌려 간다 */
    var session: CutSession? = null
        private set

    /** 마지막으로 오려낸 모양 (종이 좌표 그대로) — "더 다듬기"할 때 다시 테이블에 올림 */
    private var lastCutOnPaper: Poly? = null
    private var sessionCounter = 0

    /** 이번 결과까지 포함한 완성 횟수 (전면 광고 판단용) */
    var completedCount: Int = 0
        private set

    /** ★ 이번 결과에서 전면 광고 판단을 이미 했는지 (한 결과에 한 번만) */
    private var interstitialHandled = false

    init {
        load()
    }

    // ───────────────────────── 불러오기 ─────────────────────────

    private fun load() {
        viewModelScope.launch {
            try {
                val picture = pictureRepository.picture(pictureId)
                _uiState.value = PlayUiState(
                    loading = false,
                    picture = picture,
                    stage = Stage.SKETCH,
                    papersLeft = PaperRules.papersFor(picture)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "그림 불러오기 실패: $pictureId", e)
                _uiState.value = PlayUiState(loading = false, error = e.message ?: "그림을 불러오지 못했어요")
            }
        }
    }

    /** 결과 화면 "다시 하기" (전면 광고 판단은 afterResult 에서) */
    fun restart() {
        feedback.click()
        session = null
        lastCutOnPaper = null
        interstitialHandled = false                                              // ★
        _uiState.value = PlayUiState()
        load()
    }

    // ───────────────────────── 밑그림 단계 ─────────────────────────

    /** 밑그림에서 조각을 눌렀을 때 */
    fun selectPiece(pieceId: String) {
        val state = _uiState.value
        if (state.stage != Stage.SKETCH) return
        feedback.click()
        if (pieceId in state.placed) {
            _uiState.update { it.copy(dialog = PlayDialog.PlacedPieceMenu(pieceId)) }
            return
        }
        // 종이가 0장이면 테이블로 가기 전에 막는다
        if (state.papersLeft == 0) {
            _uiState.update { it.copy(dialog = PlayDialog.OutOfPaper) }
            return
        }
        val piece = pieceOf(pieceId) ?: return
        openTable(piece, startSheet = null)
    }

    /** 붙인 조각 메뉴 → "다시 오리기" (붙인 것을 떼어 내고 처음부터) */
    fun removePlaced(pieceId: String) {
        _uiState.update { it.copy(placed = it.placed - pieceId, dialog = null) }
    }

    // ───────────────────────── 자르기 테이블 ─────────────────────────

    private fun openTable(piece: Piece, startSheet: Poly?) {
        val paperSize = PaperRules.paperSizeFor(piece.shape)
        session = CutSession(
            paperSize = paperSize,
            targetArea = piece.shape.area(),
            startSheet = startSheet
        )
        sessionCounter++
        _uiState.update {
            it.copy(
                stage = Stage.TABLE,
                placing = null,
                table = TableUi(
                    pieceId = piece.id,
                    paperSize = paperSize.toFloat(),
                    trimming = startSheet != null,
                    sessionId = sessionCounter
                )
            )
        }
    }

    /** 새 종이에 처음 가위를 대는 순간 (TableController 가 부름) */
    fun onNeedPaper() {
        val s = session ?: return
        val state = _uiState.value
        val left = state.papersLeft
        when {
            state.table?.trimming == true -> s.markUsed()        // 다듬기는 종이 소모 없음
            left == null -> s.markUsed()                         // 연습: 무제한
            left > 0 -> {
                s.markUsed()
                _uiState.update { it.copy(papersLeft = left - 1) }
            }
            else -> _uiState.update { it.copy(dialog = PlayDialog.OutOfPaper) }
        }
    }

    /** 가위가 실제로 자르며 나아가는 프레임마다 (TableController 가 부름) */
    fun onCutting() {
        feedback.cutting()
    }

    /** 종이가 갈라졌거나(SPLIT) 구멍이 닫혔을 때(CLOSED) */
    fun onTableEvent(event: CutSession.Event) {
        when (event) {
            CutSession.Event.SPLIT -> feedback.split()
            CutSession.Event.CLOSED -> {
                feedback.closed()
                finishCut()                                      // 안쪽을 오려냈으면 바로 붙이기로
            }
            else -> Unit
        }
    }

    /**
     * 테이블을 탭했을 때 (종이 좌표).
     * 호출 뒤 화면에서 controller.refresh() 를 불러 준다.
     */
    fun onTableTap(p: Offset) {
        val s = session ?: return
        when (s.phase) {
            CutSession.Phase.READY -> s.moveStart(p)
            CutSession.Phase.CHOOSING -> {
                val choices: List<Poly> = s.choices?.toList() ?: emptyList()
                val index = choices.indexOfFirst { insidePoly(it, p) }
                if (index >= 0) {
                    s.choose(index)
                    feedback.click()
                }
            }
            else -> Unit
        }
    }

    /** "오려내기 완료" */
    fun finishCut() {
        val s = session ?: return
        s.finish()
        val result: Poly = s.result ?: return
        val pieceId = _uiState.value.table?.pieceId ?: return
        val piece = pieceOf(pieceId) ?: return

        lastCutOnPaper = result
        session = null

        // 무게중심을 (0,0)으로 옮겨 두면 회전·뒤집기가 제자리에서 된다
        val c = result.centroid()
        val local = result.map { it - c }

        _uiState.update {
            it.copy(
                stage = Stage.PLACING,
                table = null,
                placing = PlacingUi(
                    pieceId = pieceId,
                    cut = local,
                    transform = PieceTransform(position = piece.shape.centroid())
                )
            )
        }
    }

    // ───────────────────────── 붙이기 ─────────────────────────

    fun moveBy(delta: Offset) = updateTransform { it.copy(position = it.position + delta) }

    fun rotateBy(degrees: Float) = updateTransform { it.copy(rotation = (it.rotation + degrees) % 360f) }

    fun flip() {
        feedback.click()
        updateTransform { it.copy(flipped = !it.flipped) }
    }

    private fun updateTransform(block: (PieceTransform) -> PieceTransform) {
        _uiState.update { st ->
            val p = st.placing ?: return@update st
            st.copy(placing = p.copy(transform = block(p.transform)))
        }
    }

    /** "더 다듬기" → 방금 자른 조각을 다시 테이블에 (종이 소모 없음) */
    fun retrim() {
        val placing = _uiState.value.placing ?: return
        val sheet = lastCutOnPaper ?: return
        val piece = pieceOf(placing.pieceId) ?: return
        openTable(piece, startSheet = sheet)
    }

    /** "붙이기" → 채점하고 밑그림으로 */
    fun confirmPlace() {
        val state = _uiState.value
        val placing = state.placing ?: return
        val piece = pieceOf(placing.pieceId) ?: return

        val world = placing.transform.apply(placing.cut)
        val score = Scoring.score(piece.shape, world)
        feedback.paste(perfect = score.grade == Grade.PERFECT)

        val placed = state.placed + (piece.id to PlacedPiece(
            pieceId = piece.id,
            cut = placing.cut,
            transform = placing.transform,
            score = score
        ))
        lastCutOnPaper = null

        _uiState.update { it.copy(stage = Stage.SKETCH, placing = null, placed = placed) }

        if (_uiState.value.allPlaced) showResult()
    }

    // ───────────────────────── 결과 ─────────────────────────

    /** 다 붙였거나, 종이가 떨어져서 "여기까지 완성"을 눌렀을 때 */
    fun showResult() {
        val state = _uiState.value
        val picture = state.picture ?: return
        if (state.stage == Stage.RESULT) return                       // 두 번 눌러 중복 저장 방지

        val results = picture.pieces.map { piece ->
            PieceResult(piece = piece, score = state.placed[piece.id]?.score)   // 안 붙인 조각 = null
        }
        val summary = TitleMaker.summarize(picture.noun, results)

        viewModelScope.launch {
            if (!picture.practice) {
                // 기록 (별·최고 점수)
                completedCount = progressRepository.recordResult(
                    pictureId = picture.id,
                    stars = summary.stars,
                    avgCells = summary.avgCells.toFloat(),
                    title = summary.title
                )
                // 작품 (실제로 오린 모양 그대로, 밑그림 층 순서대로)
                if (state.placed.isNotEmpty()) {
                    artworkRepository.save(
                        Artwork(
                            pictureId = picture.id,
                            title = summary.title,
                            stars = summary.stars,
                            avgCells = summary.avgCells.toFloat(),
                            createdAt = System.currentTimeMillis(),
                            pieces = picture.pieces.mapNotNull { piece ->
                                state.placed[piece.id]?.let { ArtworkPiece(piece.id, it.worldShape) }
                            }
                        )
                    )
                }
            }

            val next = try {
                if (picture.practice) null else pictureRepository.catalog().nextOf(picture.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    stage = Stage.RESULT,
                    dialog = null,
                    result = ResultUi(summary = summary, pieces = results, nextPictureId = next)
                )
            }
            feedback.result(summary.stars)
        }
    }

    /**
     * ★ 결과 화면에서 다음 행동(다음 그림 / 다시 하기 / 메뉴)을 눌렀을 때.
     * 전면 광고 차례면 광고를 보여 주고, 닫힌 뒤에 action 을 실행한다.
     * 연습 그림, 광고 제거 구매자, 이미 판단한 결과는 바로 action.
     */
    fun afterResult(activity: Activity?, action: () -> Unit) {
        val picture = _uiState.value.picture
        if (activity == null || picture == null || picture.practice || interstitialHandled) {
            action()
            return
        }
        interstitialHandled = true
        adManager.maybeShowInterstitial(
            activity = activity,
            completedCount = completedCount,
            removeAds = progressRepository.progress.value.removeAds,
            onDone = action
        )
    }

    // ───────────────────────── 창 / 뒤로 ─────────────────────────

    /**
     * ★ "광고 보고 +2장" → 보상형 광고. 끝까지 봤을 때만 지급.
     * 광고가 준비 안 됐거나 중간에 닫으면 창은 그대로 (다시 시도 가능)
     */
    fun watchRewardAd(activity: Activity?) {
        if (activity == null) return
        adManager.showRewarded(activity) { earned ->
            if (earned) grantRewardPapers()
        }
    }

    private fun grantRewardPapers() {
        _uiState.update { st ->
            val left = st.papersLeft ?: return@update st.copy(dialog = null)
            st.copy(papersLeft = left + PaperRules.REWARD_PAPERS, dialog = null)
        }
        feedback.paste(perfect = true)                                           // ★ 받았다는 손맛
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = null) }
    }

    /**
     * 뒤로 가기.
     * @return true 면 화면을 닫아야 함
     */
    fun back(): Boolean {
        val state = _uiState.value
        return when (state.stage) {
            Stage.TABLE -> {                       // 자르던 종이는 버리고 밑그림으로 (쓴 종이는 돌려주지 않음)
                session = null
                _uiState.update { it.copy(stage = Stage.SKETCH, table = null) }
                false
            }
            Stage.PLACING -> {                     // 붙이기 취소 → 다시 다듬을 수 있게 테이블로
                retrim()
                false
            }
            Stage.SKETCH -> {
                if (state.placed.isEmpty() || state.dialog == PlayDialog.ConfirmExit) true
                else {
                    _uiState.update { it.copy(dialog = PlayDialog.ConfirmExit) }
                    false
                }
            }
            Stage.RESULT -> true
        }
    }

    // ───────────────────────── 도우미 ─────────────────────────

    private fun pieceOf(id: String): Piece? =
        _uiState.value.picture?.pieces?.firstOrNull { it.id == id }

    /**
     * 점이 다각형 안에 있는지 (반직선 교차법).
     * List<Offset> 에는 원래 contains(element) 멤버 함수가 있어서
     * 확장 함수 이름을 contains 로 쓰면 그쪽이 먼저 불린다 → 헷갈리지 않게 따로 둔다.
     */
    private fun insidePoly(poly: Poly, p: Offset): Boolean {
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[j]
            if ((a.y > p.y) != (b.y > p.y) &&
                p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
            ) inside = !inside
            j = i
        }
        return inside
    }

    private companion object {
        const val TAG = "PlayViewModel"
    }
}