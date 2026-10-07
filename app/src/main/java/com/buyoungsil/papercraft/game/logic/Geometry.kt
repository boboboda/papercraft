package com.buyoungsil.papercraft.game.logic

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 닫힌 다각형. 점 목록이고, 마지막 점 → 첫 점은 자동으로 이어진 것으로 본다.
 * 좌표 단위는 밑그림 기준 (300×300, 모눈 한 칸 = 10)
 */
typealias Poly = List<Offset>

/** 직사각형 다각형 (왼쪽 위 → 오른쪽 위 → 오른쪽 아래 → 왼쪽 아래) */
fun rectPoly(x1: Float, y1: Float, x2: Float, y2: Float): Poly =
    listOf(Offset(x1, y1), Offset(x2, y1), Offset(x2, y2), Offset(x1, y2))

/** 넓이 (신발끈 공식) */
fun Poly.area(): Float {
    var a = 0f
    for (i in indices) {
        val p = this[i]
        val q = this[(i + 1) % size]
        a += p.x * q.y - q.x * p.y
    }
    return abs(a / 2f)
}

/** 무게중심. 넓이가 0이면 점들의 평균 */
fun Poly.centroid(): Offset {
    var a = 0f
    var cx = 0f
    var cy = 0f
    for (i in indices) {
        val p = this[i]
        val q = this[(i + 1) % size]
        val f = p.x * q.y - q.x * p.y
        a += f
        cx += (p.x + q.x) * f
        cy += (p.y + q.y) * f
    }
    if (abs(a) < 1e-6f) {
        return Offset(sumOf { it.x.toDouble() }.toFloat() / size, sumOf { it.y.toDouble() }.toFloat() / size)
    }
    return Offset(cx / (3f * a), cy / (3f * a))
}

/**
 * ★ 점이 다각형 안에 있는지 (반직선을 그어 테두리와 몇 번 만나는지 셈)
 * 이름을 contains 로 하면 List.contains(원소) 멤버 함수가 먼저 불려서
 * 이 함수는 절대 실행되지 않는다 → 반드시 다른 이름을 쓴다.
 */
fun Poly.containsPoint(p: Offset): Boolean {
    var inside = false
    var j = size - 1
    for (i in indices) {
        val a = this[i]
        val b = this[j]
        if ((a.y > p.y) != (b.y > p.y) &&
            p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
        ) inside = !inside
        j = i
    }
    return inside
}

/**
 * 테두리 위의 한 점.
 * @param pos 실제 좌표
 * @param u 둘레 위치 = 변 번호 + 그 변 위의 비율(0~1). 둘레를 따라 걸을 때 쓴다.
 * @param inwardAngle 그 변에서 다각형 안쪽을 향하는 각도 (가위가 처음 바라보는 방향)
 */
data class BoundaryPoint(val pos: Offset, val u: Float, val inwardAngle: Float)

/** p에서 가장 가까운 테두리 위의 점 (가위 출발 위치) */
fun Poly.nearestOnBoundary(p: Offset): BoundaryPoint {
    var bestI = 0
    var bestT = 0f
    var bestD = Float.MAX_VALUE
    for (i in indices) {
        val a = this[i]
        val d = this[(i + 1) % size] - a
        val len2 = (d.x * d.x + d.y * d.y).coerceAtLeast(1e-9f)
        val t = (((p.x - a.x) * d.x + (p.y - a.y) * d.y) / len2).coerceIn(0f, 1f)
        val dist = (a + d * t - p).getDistanceSquared()
        if (dist < bestD) {
            bestD = dist; bestI = i; bestT = t
        }
    }
    val a = this[bestI]
    val b = this[(bestI + 1) % size]
    val d = b - a
    val len = d.getDistance().coerceAtLeast(1e-6f)

    // 꼭짓점 바로 위에서 출발하면 안쪽 방향이 애매해서 가위가 못 움직임 → 변 안쪽으로 살짝 옮김
    val nudge = min(2f, len * 0.3f) / len
    val t = bestT.coerceIn(nudge, 1f - nudge)

    // 변에 수직인 방향 중 다각형 안쪽을 향하는 쪽 선택
    var n = Offset(-d.y / len, d.x / len)
    if (!containsPoint((a + b) / 2f + n * max(0.3f, len * 0.01f))) n = -n   // ★

    return BoundaryPoint(a + d * t, bestI + t, atan2(n.y, n.x))
}

/** from → to 선분이 테두리와 처음 만나는 점 (가위가 종이 끝에 닿은 위치) */
fun Poly.hitEdge(from: Offset, to: Offset): BoundaryPoint {
    val r = to - from
    var bestT = Float.MAX_VALUE
    var hit: BoundaryPoint? = null
    val eps = 1e-4f
    for (i in indices) {
        val c = this[i]
        val s = this[(i + 1) % size] - c
        val den = r.x * s.y - r.y * s.x
        if (abs(den) < 1e-9f) continue      // 평행
        val q = c - from
        val t = (q.x * s.y - q.y * s.x) / den   // 이동 선분 위 비율
        val u = (q.x * r.y - q.y * r.x) / den   // 테두리 변 위 비율
        if (t in -eps..1f + eps && u in -eps..1f + eps && t < bestT) {
            bestT = t
            hit = BoundaryPoint(from + r * t, i + u.coerceIn(0f, 1f - 1e-6f), 0f)
        }
    }
    return hit ?: nearestOnBoundary(to)
}

/** 연달아 겹친 점 제거 (자르기 경로 정리용) */
fun Poly.dedupe(): Poly {
    val out = ArrayList<Offset>(size)
    for (p in this) {
        if (out.isEmpty() || (p - out.last()).getDistance() > 1e-3f) out += p
    }
    if (out.size > 2 && (out.first() - out.last()).getDistance() < 1e-3f) out.removeAt(out.lastIndex)
    return out
}

/** 테두리를 step 간격의 점들로 촘촘히 (판정용) */
fun Poly.resample(step: Float): List<Offset> {
    val out = ArrayList<Offset>()
    for (i in indices) {
        val a = this[i]
        val b = this[(i + 1) % size]
        val k = max(1, ceil((b - a).getDistance() / step).toInt())
        for (j in 0 until k) out += a + (b - a) * (j.toFloat() / k)
    }
    return out
}

/** 점 p에서 테두리까지 최단거리 (판정용) */
fun Poly.distanceTo(p: Offset): Float {
    var best = Float.MAX_VALUE
    for (i in indices) {
        val a = this[i]
        val d = this[(i + 1) % size] - a
        val len2 = (d.x * d.x + d.y * d.y).coerceAtLeast(1e-9f)
        val t = (((p.x - a.x) * d.x + (p.y - a.y) * d.y) / len2).coerceIn(0f, 1f)
        best = min(best, (a + d * t - p).getDistanceSquared())
    }
    return sqrt(best)
}