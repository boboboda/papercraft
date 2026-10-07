package com.buyoungsil.papercraft.data.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

/**
 * 내 작품 1점 = 완성했을 때 실제로 오려 붙인 모양 그대로.
 * 갤러리에서 다시 그릴 수 있게 조각별 "밑그림 좌표(300×300)" 다각형을 저장한다.
 * 색은 저장하지 않는다 → pictureId 로 그림을 불러와 조각 id 의 색을 쓴다.
 */
data class Artwork(
    val id: String = "",                // Firestore 문서 id (저장 전엔 빈 값)
    val pictureId: String,
    val title: String,                  // 〈귀가 없는 고양이〉 의 안쪽 글자
    val stars: Int,
    val avgCells: Float,
    val createdAt: Long,
    val pieces: List<ArtworkPiece>      // 붙인 조각만 (안 붙인 조각은 없음 = 진짜로 없는 그림)
)

data class ArtworkPiece(
    val pieceId: String,
    val points: List<Offset>            // 밑그림 좌표
)

// ───────────────────────── 저장용 변환 ─────────────────────────
// Firestore 는 "배열 안의 배열"을 저장할 수 없다 → [[x,y],[x,y]] 대신 [x,y,x,y,...] 평평하게.
// 소수점 한 자리로 줄여서 용량도 줄인다 (0.1 = 1칸의 1/100, 눈에 안 보이는 차이).

/** [(1.234, 5.678), ...] → [1.2, 5.7, ...] */
fun List<Offset>.toFlat(): List<Double> = flatMap { listOf(round1(it.x), round1(it.y)) }

/** [1.2, 5.7, ...] → [(1.2, 5.7), ...] (숫자 개수가 홀수면 마지막 하나는 버림) */
fun List<Number>.toOffsets(): List<Offset> =
    chunked(2).filter { it.size == 2 }.map { (x, y) -> Offset(x.toFloat(), y.toFloat()) }

/**
 * 거의 일직선 위에 있는 점 빼기 (더글라스-포이커).
 * 가위 자국은 점이 아주 촘촘해서, 저장 전에 줄이면 문서 크기가 크게 준다.
 * @param tolerance 이 거리(밑그림 좌표)보다 덜 벗어난 점은 지움. 0.3 = 1칸의 3%
 */
fun List<Offset>.simplify(tolerance: Float = 0.3f): List<Offset> {
    if (size < 4) return this
    val keep = BooleanArray(size)
    keep[0] = true
    keep[lastIndex] = true

    // ★ apply { add(0 to lastIndex) } 로 쓰면 lastIndex 가 "빈 덱"의 것(-1)으로 읽혀
    //    아무 점도 검사하지 않고 양 끝 2점만 남았다 → 바깥에서 따로 넣는다
    val last = lastIndex
    val stack = ArrayDeque<Pair<Int, Int>>()
    stack.add(0 to last)                                                           // ★

    while (stack.isNotEmpty()) {
        val (from, to) = stack.removeLast()
        var maxDist = 0f
        var maxIndex = -1
        for (i in from + 1 until to) {
            val d = distanceToSegment(this[i], this[from], this[to])
            if (d > maxDist) { maxDist = d; maxIndex = i }
        }
        if (maxIndex != -1 && maxDist > tolerance) {
            keep[maxIndex] = true
            stack.add(from to maxIndex)
            stack.add(maxIndex to to)
        }
    }
    return filterIndexed { i, _ -> keep[i] }
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}

private fun round1(v: Float): Double = (v * 10).roundToInt() / 10.0