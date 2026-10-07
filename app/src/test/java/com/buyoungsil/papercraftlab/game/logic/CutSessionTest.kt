package com.buyoungsil.papercraftlab.game.logic

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

/**
 * 자르기 과정 전체를 프레임 단위로 시뮬레이션.
 *
 * 종이 100×100 → 가위 속도 20/초, dt 0.05초 → 한 프레임에 1씩 전진.
 * 가위는 아래 가운데 (50, 100)에서 위를 보고 출발.
 */
class CutSessionTest {

    private val dt = 0.05f

    /** 자르기 버튼만 n프레임 누르기 (방향 그대로) */
    private fun CutSession.cutStraight(frames: Int) {
        repeat(frames) { step(dt, targetAngle = null, cutting = true) }
    }

    @Test
    fun 색종이를_쓰기_전에는_가위가_움직이지_않음() {
        val s = CutSession(100f, 3600f)
        val before = s.position
        s.cutStraight(20)
        assertEquals(before, s.position)
        assertEquals(CutSession.Phase.READY, s.phase)
    }

    @Test
    fun 위로_60_자르면_y가_40() {
        val s = CutSession(100f, 3600f)
        s.markUsed()
        s.cutStraight(60)
        assertEquals(50f, s.position.x, 0.5f)
        assertEquals(40f, s.position.y, 0.5f)
        assertEquals(CutSession.Phase.CUTTING, s.phase)
    }

    @Test
    fun 위로_자르다_오른쪽으로_꺾어_끝까지_가면_두_쪽_중_고르고_완료() {
        val s = CutSession(100f, 3600f)
        s.markUsed()
        s.cutStraight(60)

        // 조이스틱을 오른쪽(0°)으로 → 곡선으로 꺾이며 오른쪽 끝까지
        var frames = 0
        var event = CutSession.Event.NONE
        while (s.phase != CutSession.Phase.CHOOSING && frames++ < 200) {
            event = s.step(dt, targetAngle = 0f, cutting = true)
        }
        assertEquals(CutSession.Event.SPLIT, event)
        val choices = assertNotNull(s.choices).let { s.choices!! }

        val a = choices.first.area()
        val b = choices.second.area()
        assertEquals("두 쪽 합 = 종이 전체", 10000f, a + b, 5f)

        // 오른쪽 아래 작은 쪽 (꺾을 때 곡선이 생겨서 3000보다 조금 큼, 실측 약 3170)
        val small = minOf(a, b)
        assertTrue("작은 쪽 넓이 $small", small in 3000f..3400f)

        // 작은 쪽 남기기 → 다시 자를 수 있는 상태, 완료 가능
        s.choose(if (a < b) 0 else 1)
        assertEquals(CutSession.Phase.READY, s.phase)
        assertEquals(1, s.cuts)
        assertTrue(s.canFinish)

        // 오려내기 완료
        assertTrue(s.finish())
        assertEquals(CutSession.Phase.CLOSED, s.phase)
        assertEquals(small, s.result!!.area(), 1f)
    }

    @Test
    fun 출발하자마자_바깥을_향하면_막힘() {
        val s = CutSession(100f, 3600f)
        s.markUsed()
        // 제자리에서 아래(종이 바깥)로 돌리기만 함
        repeat(20) { s.step(dt, targetAngle = (PI / 2).toFloat(), cutting = false) }
        val before = s.position
        s.cutStraight(10)
        assertEquals(before, s.position)
        assertEquals(CutSession.Phase.READY, s.phase)
    }

    @Test
    fun 종이_안에서_한_바퀴_돌면_고리_안쪽이_오려짐() {
        val s = CutSession(100f, targetArea = 400f)
        s.markUsed()
        s.cutStraight(20)   // 종이 안쪽으로 조금 들어감

        // 조이스틱을 계속 오른쪽으로 돌림 → 최대 회전 속도로 작은 원 (반지름 약 4)
        var frames = 0
        var event = CutSession.Event.NONE
        while (event != CutSession.Event.CLOSED && frames++ < 300) {
            event = s.step(dt, targetAngle = s.angle + (PI / 2).toFloat(), cutting = true)
        }
        assertEquals(CutSession.Event.CLOSED, event)
        assertEquals(CutSession.Phase.CLOSED, s.phase)
        assertEquals(50f, s.result!!.area(), 15f)   // π × 4² ≈ 50 (실측 51)
    }

    @Test
    fun 더_다듬기는_오린_조각에서_바로_시작() {
        val piece = rectPoly(20f, 20f, 80f, 80f)
        val s = CutSession(100f, 3600f, startSheet = piece)

        assertTrue("이미 색종이를 쓴 상태", s.used)
        assertTrue("자르지 않고도 바로 완료 가능", s.canFinish)
        assertEquals(Offset(50f, 80f), s.position)   // 조각 아래 가운데에서 출발
    }

    @Test
    fun 가장자리를_탭하면_출발_위치_이동_멀면_무시() {
        val s = CutSession(100f, 3600f)

        assertTrue(s.moveStart(Offset(0f, 50f)))        // 왼쪽 변
        assertEquals(Offset(0f, 50f), s.position)
        assertEquals(0f, s.angle, 0.001f)                // 오른쪽(안쪽)을 봄

        assertFalse(s.moveStart(Offset(50f, 50f)))      // 종이 한가운데는 가장자리에서 너무 멂
        assertEquals(Offset(0f, 50f), s.position)
    }
}