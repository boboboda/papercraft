package com.buyoungsil.papercraft.feature.play.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import com.buyoungsil.papercraft.game.logic.CutSession

/**
 * 자르기 테이블의 심장. 매 프레임:
 *   조이스틱 방향 + 버튼 눌림 → CutSession.step() → 바뀌었으면 다시 그리기 신호
 *
 * - CutSession 은 PlayViewModel 이 들고 있다 (화면 회전에도 살아남게). 여기선 빌려 쓰기만.
 * - 매 프레임 StateFlow 를 쏘지 않는다.
 *   · frame     : 세션이 바뀐 프레임(버전·위치·각도)에만 +1 → 테이블 Canvas 만 다시 그림
 *   · phase     : 단계가 실제로 바뀔 때만 갱신 → 정보 줄 문구만 다시 그림
 *   · canFinish : 바뀔 때만 갱신 → "오려내기 완료" 버튼만 다시 그림
 */
@Stable
class TableController(
    val joystick: JoystickState,
    val cutButton: CutButtonState
) {
    /** 지금 자르고 있는 종이. null 이면 루프는 돌지만 아무 일도 안 함 */
    var session: CutSession? = null
        private set

    /** 테이블 Canvas 가 읽는 값. 이 값이 바뀌면 Canvas 가 다시 그려진다 */
    var frame by mutableIntStateOf(0)
        private set

    /** 현재 단계 (READY / CUTTING / CHOOSING / CLOSED). 세션 없으면 null */
    var phase by mutableStateOf<CutSession.Phase?>(null)
        private set

    /** "오려내기 완료" 버튼 활성화 여부 */
    var canFinish by mutableStateOf(false)
        private set

    private var lastVersion = -1L
    private var lastPosition = Offset.Unspecified                 // ★
    private var lastAngle = Float.NaN                             // ★
    private var askedPaperThisPress = false

    /**
     * 화면이 떠 있는 동안 계속 도는 루프. LaunchedEffect 안에서 부른다.
     *
     * @param onNeedPaper 새 종이에 처음 가위를 대는 순간 1번 호출.
     *                    ViewModel 이 남은 장수를 확인하고 session.markUsed() 를 부른다.
     * @param onCutting   가위가 실제로 종이를 자르며 나아가는 프레임마다 호출 (소리·진동용)
     * @param onEvent     종이가 갈라졌거나(SPLIT) 구멍이 닫혔을 때(CLOSED)
     */
    suspend fun run(
        onNeedPaper: () -> Unit,
        onCutting: () -> Unit,
        onEvent: (CutSession.Event) -> Unit
    ) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 0f
                else ((now - lastNanos) / 1_000_000_000f).coerceAtMost(MAX_DT)
                lastNanos = now

                val s = session ?: return@withFrameNanos
                val cutting = cutButton.isPressed

                // 종이 소모는 "처음 가위를 댄 순간" 한 번만 물어본다
                if (!cutting) askedPaperThisPress = false
                if (cutting && !s.used && !askedPaperThisPress) {
                    askedPaperThisPress = true
                    onNeedPaper()
                }

                val event = s.step(dt, joystick.direction, cutting)

                // ★ 버전뿐 아니라 가위 위치·각도가 바뀌어도 "변했다"로 본다
                val pos = s.position
                val ang = s.angle.toFloat()
                val moved = pos != lastPosition
                val changed = s.version != lastVersion || moved || ang != lastAngle

                // 버튼 누름 + 종이 받음 + 자르는 단계 + 실제로 이동 → 사각사각
                if (cutting && s.used && s.phase == CutSession.Phase.CUTTING && moved) {
                    onCutting()
                }

                if (changed) {
                    lastVersion = s.version
                    lastPosition = pos
                    lastAngle = ang
                    frame++
                }
                syncFlags(s)
                if (event != CutSession.Event.NONE) onEvent(event)
            }
        }
    }

    /** 새 종이로 바꿀 때 (ViewModel 이 새 CutSession 을 만든 직후) */
    fun attach(newSession: CutSession?) {
        session = newSession
        lastVersion = -1L
        lastPosition = Offset.Unspecified                         // ★
        lastAngle = Float.NaN                                     // ★
        askedPaperThisPress = false
        frame++
        if (newSession != null) syncFlags(newSession) else {
            phase = null
            canFinish = false
        }
    }

    /**
     * 바깥(ViewModel)에서 세션을 직접 건드린 뒤 호출
     * (조각 고르기 choose(), 시작점 옮기기 moveStart() 등은 루프 밖에서 일어나므로)
     */
    fun refresh() {
        session?.let {
            frame++
            syncFlags(it)
        }
    }

    // 값이 다를 때만 쓴다 → 같은 값이면 아무것도 다시 그려지지 않음
    private fun syncFlags(s: CutSession) {
        if (phase != s.phase) phase = s.phase
        if (canFinish != s.canFinish) canFinish = s.canFinish
    }

    private companion object {
        /** 렉이 걸려 한 프레임이 길어져도 가위가 순간이동하지 않게 (최대 1/20초) */
        const val MAX_DT = 0.05f
    }
}

@Composable
fun rememberTableController(
    joystick: JoystickState = rememberJoystickState(),
    cutButton: CutButtonState = rememberCutButtonState()
): TableController = remember(joystick, cutButton) { TableController(joystick, cutButton) }

/**
 * 테이블 화면에 한 줄 넣으면 루프가 돈다.
 * 화면을 벗어나면 LaunchedEffect 가 취소되면서 루프도 멈춘다.
 *
 * @param sessionId PlayUiState.table.sessionId — 바뀌면 새 세션으로 갈아 끼운다
 */
@Composable
fun TableLoop(
    controller: TableController,
    session: CutSession?,
    sessionId: Int,
    onNeedPaper: () -> Unit,
    onCutting: () -> Unit = {},
    onEvent: (CutSession.Event) -> Unit
) {
    // 콜백이 바뀌어도 루프를 재시작하지 않도록 최신 값만 참조
    val needPaper by rememberUpdatedState(onNeedPaper)
    val cutting by rememberUpdatedState(onCutting)
    val event by rememberUpdatedState(onEvent)

    LaunchedEffect(sessionId) { controller.attach(session) }

    LaunchedEffect(controller) {
        controller.run(
            onNeedPaper = { needPaper() },
            onCutting = { cutting() },
            onEvent = { event(it) }
        )
    }
}