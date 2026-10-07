package com.buyoungsil.papercraftlab.game.logic

import androidx.compose.ui.geometry.Offset
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.test.assertNotEquals

/**
 * Geometry.kt, PaperSplit.kt 검증.
 * 기대값은 HTML 프로토타입에서 같은 입력으로 계산한 값과 같다.
 */
class GeometryTest {

    /** 100×100 정사각형 종이. 꼭짓점 0:왼위, 1:오위, 2:오아래, 3:왼아래 */
    private val square = rectPoly(0f, 0f, 100f, 100f)

    // ---------- 기본 계산 ----------

    @Test
    fun 넓이() {
        assertEquals(10000f, square.area(), 0.01f)
        val triangle = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(0f, 10f))
        assertEquals(50f, triangle.area(), 0.01f)
    }

    @Test
    fun 무게중심() {
        val c = square.centroid()
        assertEquals(50f, c.x, 0.01f)
        assertEquals(50f, c.y, 0.01f)
    }

    @Test
    fun 안팎_판정() {
        assertTrue(square.contains(Offset(50f, 50f)))
        assertFalse(square.contains(Offset(150f, 50f)))
        assertFalse(square.contains(Offset(50f, -1f)))
    }

    @Test
    fun 테두리까지_거리() {
        assertEquals(50f, square.distanceTo(Offset(50f, 50f)), 0.01f)
        assertEquals(10f, square.distanceTo(Offset(110f, 50f)), 0.01f)
    }

    @Test
    fun 테두리_촘촘히_찍기() {
        // 한 변 100을 10 간격으로 → 변마다 10개 × 4변
        assertEquals(40, square.resample(10f).size)
    }

    // ---------- 가위 출발 위치 ----------

    @Test
    fun 아래_가운데에서_위를_보고_출발() {
        val bp = square.nearestOnBoundary(Offset(50f, 100f))
        assertEquals(Offset(50f, 100f), bp.pos)
        assertEquals(-PI.toFloat() / 2f, bp.inwardAngle, 0.001f)  // 위쪽 = -90°
    }

    @Test
    fun 꼭짓점에서는_변_안쪽으로_살짝_옮김() {
        val bp = square.nearestOnBoundary(Offset(0f, 100f))
        assertNotEquals(Offset(0f, 100f), bp.pos)
        assertEquals(100f, bp.pos.y, 0.01f)  // 여전히 아래 변 위
    }

    @Test
    fun 가장자리에_닿는_점() {
        val hit = square.hitEdge(Offset(99f, 40f), Offset(110f, 40f))
        assertEquals(100f, hit.pos.x, 0.01f)
        assertEquals(40f, hit.pos.y, 0.01f)
    }

    // ---------- 종이 나누기 ----------

    @Test
    fun 아래에서_위로_자르고_오른쪽_끝까지_가면_3000과_7000으로_나뉨() {
        val start = square.nearestOnBoundary(Offset(50f, 100f))
        val hit = square.hitEdge(Offset(99f, 40f), Offset(110f, 40f))
        val path = listOf(Offset(50f, 100f), Offset(50f, 40f), hit.pos)

        val (a, b) = splitSheet(square, path, start.u, hit.u)
        val areas = listOf(a.area(), b.area()).sorted()

        assertEquals(3000f, areas[0], 1f)
        assertEquals(7000f, areas[1], 1f)
    }

    @Test
    fun 같은_변으로_돌아오면_작은_사각형이_떨어짐() {
        val start = square.nearestOnBoundary(Offset(20f, 100f))
        val hit = square.hitEdge(Offset(40f, 70f), Offset(40f, 110f))
        val path = listOf(Offset(20f, 100f), Offset(20f, 70f), Offset(40f, 70f), hit.pos)

        val (a, b) = splitSheet(square, path, start.u, hit.u)
        val areas = listOf(a.area(), b.area()).sorted()

        assertEquals(600f, areas[0], 1f)   // 20 × 30
        assertEquals(9400f, areas[1], 1f)
    }
}