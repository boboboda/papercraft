package com.buyoungsil.papercraft.game.logic

import androidx.compose.ui.geometry.Offset
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 종이를 자른 선으로 두 쪽으로 나눈다.
 *
 * 원리:
 *  가위는 테두리 위 startU에서 출발해서 테두리 위 endU에서 끝났다.
 *  - 한 쪽 = 자른 선 + (endU에서 startU까지 테두리를 시계 방향으로 걸으며 지나는 꼭짓점)
 *  - 다른 쪽 = 자른 선 + (같은 구간을 반시계 방향으로 걸으며 지나는 꼭짓점)
 *
 * @param sheet 현재 종이 모양
 * @param path 자른 선 (첫 점은 startU 위치, 마지막 점은 endU 위치)
 * @param startU 출발 지점의 둘레 위치 (변 번호 + 비율)
 * @param endU 도착 지점의 둘레 위치
 */
fun splitSheet(sheet: Poly, path: List<Offset>, startU: Float, endU: Float): Pair<Poly, Poly> {
    val forward = (path + sheet.walk(from = endU, to = startU, dir = 1)).dedupe()
    val backward = (path + sheet.walk(from = endU, to = startU, dir = -1)).dedupe()
    return forward to backward
}

/** 둘레 위치 a에서 b까지 정방향으로 걸은 거리 (변 개수 단위, 0 ≤ 결과 < n) */
private fun forwardDistance(a: Float, b: Float, n: Int): Float = ((b - a) % n + n) % n

/**
 * 둘레를 from 위치에서 to 위치까지 걸으면서 지나치는 꼭짓점들을 순서대로 모은다.
 * from, to 자체(변 중간의 점)는 포함하지 않는다. 그 점들은 자른 선의 양 끝에 이미 있다.
 *
 * @param dir 1 = 꼭짓점 번호가 커지는 방향, -1 = 작아지는 방향
 */
internal fun Poly.walk(from: Float, to: Float, dir: Int): List<Offset> {
    val n = size
    val out = mutableListOf<Offset>()

    if (dir > 0) {
        val total = forwardDistance(from, to, n)
        for (m in 0 until n) {
            val k = (floor(from).toInt() + 1 + m) % n          // 다음 꼭짓점
            val d = forwardDistance(from, k.toFloat(), n)
            if (d >= total) break                                // 도착 지점을 지나면 멈춤
            if (d > 0f) out += this[k]
        }
    } else {
        val total = forwardDistance(to, from, n)
        for (m in 0 until n) {
            val k = ((ceil(from).toInt() - 1 - m) % n + n) % n // 이전 꼭짓점
            val d = forwardDistance(k.toFloat(), from, n)
            if (d >= total) break
            if (d > 0f) out += this[k]
        }
    }
    return out
}