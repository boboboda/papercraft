package com.buyoungsil.papercraftlab.game.logic

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 색종이 한 장을 자르는 과정 전체.
 *
 * 화면은 이 객체의 값을 읽어서 그리기만 하고,
 * 입력은 step() / choose() / moveStart() / finish() 로만 넣는다.
 * 종이 한 장마다 새로 만든다. (화면 회전 시 날아가지 않게 PlayViewModel이 보관)
 *
 * 상태 흐름:
 *   READY(출발 대기) → CUTTING(자르는 중)
 *     ├ 가장자리에 닿음 → CHOOSING(두 쪽 중 선택) → choose() → READY (계속 다듬기)
 *     ├ 선이 한 바퀴 이어짐 → CLOSED(오려냄)
 *   READY에서 finish() → CLOSED
 *
 * @param paperSize 색종이 한 변 길이 (밑그림과 같은 단위)
 * @param targetArea 목표 조각 넓이. 너무 작은 고리를 조각으로 인정하지 않기 위해 사용
 * @param startSheet "더 다듬기"로 이미 오린 조각을 다시 자를 때 그 모양 (새 종이면 null)
 */
class CutSession(
    val paperSize: Float,
    private val targetArea: Float,
    startSheet: Poly? = null,
) {
    enum class Phase { READY, CUTTING, CHOOSING, CLOSED }

    /** step()이 알려주는 사건. 소리·진동·화면 전환을 여기에 맞춰 낸다 */
    enum class Event { NONE, SPLIT, CLOSED }

    /** 가위 속도: 종이 한 변을 5초에 가로지름 */
    val speed: Float = paperSize / 5f

    /** 현재 남아 있는 종이 모양 (자를수록 작아짐) */
    var sheet: Poly = startSheet ?: rectPoly(0f, 0f, paperSize, paperSize)
        private set

    private val _path = mutableListOf<Offset>()
    /** 지금 자르고 있는 선 */
    val path: List<Offset> get() = _path

    /** 가위 위치와 방향(라디안, 0 = 오른쪽, 시계 방향 증가) */
    var position: Offset = Offset.Zero
        private set
    var angle: Float = 0f
        private set

    /** 이번 선에서 자른 길이. 0이면 아직 출발 전 */
    var pathLength: Float = 0f
        private set

    /** 종이를 나눈 횟수. 1 이상이어야 "오려내기 완료" 가능 */
    var cuts: Int = if (startSheet != null) 1 else 0
        private set

    /** 이 종이에 가위를 댔는지. ViewModel이 색종이를 한 장 줄이고 markUsed()를 부른다 */
    var used: Boolean = startSheet != null
        private set

    /** 가장자리에 닿아 나뉜 두 쪽 (CHOOSING일 때만 값이 있음) */
    var choices: Pair<Poly, Poly>? = null
        private set

    /** 최종 오린 조각 (CLOSED일 때만 값이 있음) */
    var result: Poly? = null
        private set

    /** 자르면서 꺾은 총 각도 (연습 판 안내용) */
    var turned: Float = 0f
        private set

    /** 값이 바뀔 때마다 증가. 화면이 다시 그릴지 판단하는 용도 */
    var version: Long = 0L
        private set

    private var startU = 0f

    val phase: Phase
        get() = when {
            result != null -> Phase.CLOSED
            choices != null -> Phase.CHOOSING
            pathLength > 0f -> Phase.CUTTING
            else -> Phase.READY
        }

    /** 한 번 이상 나눈 뒤, 자르는 중이 아닐 때만 "오려내기 완료" 가능 */
    val canFinish: Boolean get() = phase == Phase.READY && cuts > 0

    init {
        setStart(defaultStart())
    }

    fun markUsed() {
        used = true
    }

    /**
     * 매 프레임 호출.
     * @param dt 지난 프레임부터 흐른 시간(초)
     * @param targetAngle 조이스틱 방향. null이면 현재 방향 유지
     * @param cutting 자르기 버튼을 누르고 있는지
     */
    fun step(dt: Float, targetAngle: Float?, cutting: Boolean): Event {
        if (phase == Phase.CHOOSING || phase == Phase.CLOSED) return Event.NONE
        var changed = false

        // 1) 방향 돌리기: 목표 방향으로 최대 TURN_SPEED만큼만 (순간 회전 X)
        if (targetAngle != null) {
            val diff = atan2(sin(targetAngle - angle), cos(targetAngle - angle)) // -π ~ π
            val maxTurn = TURN_SPEED * dt
            val da = diff.coerceIn(-maxTurn, maxTurn)
            if (da != 0f) {
                angle += da
                if (pathLength > 0f) turned += abs(da)
                changed = true
            }
        }

        // 2) 앞으로 자르기
        var event = Event.NONE
        if (cutting && used) {
            val next = Offset(
                position.x + cos(angle) * speed * dt,
                position.y + sin(angle) * speed * dt,
            )
            // sheet.contains(next) 는 List 의 "원소 포함" 멤버 함수가 불려서 항상 false 였음
            if (isInside(sheet, next)) {
                pathLength += (next - position).getDistance()
                position = next
                changed = true
                // 너무 촘촘하게 쌓지 않도록 일정 거리마다만 점 기록
                if ((position - _path.last()).getDistance() > paperSize / 250f) {
                    _path += position
                    if (checkClose()) event = Event.CLOSED
                }
            } else if (pathLength >= paperSize * 0.03f) {
                // 종이 가장자리에 닿음 → 종이 나누기
                val hit = sheet.hitEdge(position, next)
                _path += hit.pos
                position = hit.pos
                changed = true
                event = reachEdge(hit.u)
            }
            // 출발 직후 바깥을 향하고 있으면 그냥 막힘 (움직이지 않음)
        }

        if (changed) version++
        return event
    }

    /** 나뉜 두 쪽 중 남길 쪽 선택 (0 = first, 1 = second) */
    fun choose(index: Int) {
        val c = choices ?: return
        keep(if (index == 0) c.first else c.second)
    }

    /** 자르기 전에 가장자리를 탭해서 가위 출발 위치 옮기기. 너무 멀면 무시 */
    fun moveStart(p: Offset): Boolean {
        if (phase != Phase.READY) return false
        val bp = sheet.nearestOnBoundary(p)
        if ((bp.pos - p).getDistance() > paperSize * 0.15f) return false
        setStart(bp)
        return true
    }

    /** "오려내기 완료" — 지금 남은 종이를 조각으로 확정 */
    fun finish(): Boolean {
        if (!canFinish) return false
        close(sheet)
        return true
    }

    // ---------------- 내부 ----------------

    private fun reachEdge(endU: Float): Event {
        val (a, b) = splitSheet(sheet, _path, startU, endU)
        val tiny = paperSize * paperSize * 0.002f   // 종이 넓이의 0.2% 미만은 부스러기
        when {
            a.area() < tiny -> keep(b)
            b.area() < tiny -> keep(a)
            else -> choices = a to b
        }
        return Event.SPLIT
    }

    private fun keep(poly: Poly) {
        val end = _path.last()
        sheet = poly
        cuts++
        choices = null
        // 다음 자르기는 방금 끝난 지점 근처 가장자리에서 출발
        setStart(sheet.nearestOnBoundary(end))
    }

    /** 자른 선이 앞서 자른 선과 만나 고리가 되면 그 안쪽을 오려냄 (원·눈 같은 조각용) */
    private fun checkClose(): Boolean {
        val n = _path.size
        if (n < 30) return false
        val last = _path[n - 1]
        val tolerance = max(1f, paperSize * 0.02f)
        for (i in 0 until n - 25) {   // 방금 지나온 점들과는 비교하지 않음
            if ((last - _path[i]).getDistance() < tolerance) {
                val loop = _path.subList(i, n).toList()
                if (loop.area() > targetArea * 0.05f) {
                    close(loop)
                    return true
                }
            }
        }
        return false
    }

    private fun close(poly: Poly) {
        result = poly.dedupe()
        version++
    }

    private fun setStart(bp: BoundaryPoint) {
        position = bp.pos
        startU = bp.u
        // ★ 계산된 "안쪽" 방향으로 살짝 나가 본 점이 종이 밖이면 반대로 뒤집는다
        //    (종이 모양·꼭짓점 순서에 따라 바깥쪽이 나오는 경우가 있었음)
        val a = bp.inwardAngle
        val probeStep = paperSize * 0.01f
        val probe = Offset(bp.pos.x + cos(a) * probeStep, bp.pos.y + sin(a) * probeStep)
        angle = if (isInside(sheet, probe)) a else a + PI.toFloat()
        _path.clear()
        _path += bp.pos
        pathLength = 0f
        version++
    }

    /** 기본 출발 위치: 종이 아래쪽 가운데 */
    private fun defaultStart(): BoundaryPoint {
        val minX = sheet.minOf { it.x }
        val maxX = sheet.maxOf { it.x }
        val maxY = sheet.maxOf { it.y }
        return sheet.nearestOnBoundary(Offset((minX + maxX) / 2f, maxY))
    }

    companion object {
        /** 가위 최대 회전 속도 (라디안/초, 약 288°/초) */
        val TURN_SPEED: Float = (PI * 1.6).toFloat()

        /**
         * 점이 다각형 안에 있는지 (반직선 교차법).
         * 이름을 contains 로 하면 List.contains(원소) 와 겹쳐서 엉뚱한 함수가 불리므로 다른 이름을 쓴다.
         */
        fun isInside(poly: Poly, p: Offset): Boolean {
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
    }
}