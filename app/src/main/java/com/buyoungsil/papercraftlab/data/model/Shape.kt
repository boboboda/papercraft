package com.buyoungsil.papercraftlab.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.buyoungsil.papercraftlab.game.logic.Poly
import com.buyoungsil.papercraftlab.game.logic.area
import com.buyoungsil.papercraftlab.game.logic.centroid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 밑그림 조각의 목표 모양.
 * 좌표는 밑그림 기준 (300×300, 모눈 한 칸 = 10)
 *
 * sealed interface: 모양 종류가 이 두 가지뿐이라고 컴파일러에 알려줌.
 * when으로 분기할 때 else 없이도 빠짐없이 처리했는지 검사해준다.
 */
sealed interface Shape {

    /** 다각형 (사각형, 삼각형, 사다리꼴 …) */
    data class Polygon(val points: Poly) : Shape

    /** 타원 (원은 rx == ry) */
    data class Ellipse(val center: Offset, val rx: Float, val ry: Float) : Shape
}

/** 테두리를 점 목록으로. 타원은 72개 점(5도 간격)으로 근사 */
fun Shape.outline(): Poly = when (this) {
    is Shape.Polygon -> points
    is Shape.Ellipse -> List(ELLIPSE_SEGMENTS) { i ->
        val a = i.toFloat() / ELLIPSE_SEGMENTS * 2f * PI.toFloat()
        Offset(center.x + rx * cos(a), center.y + ry * sin(a))
    }
}

/** 넓이 (타원은 공식으로 정확하게) */
fun Shape.area(): Float = when (this) {
    is Shape.Polygon -> points.area()
    is Shape.Ellipse -> PI.toFloat() * rx * ry
}

/** 중심 (붙일 위치의 기준점, 라벨 위치) */
fun Shape.centroid(): Offset = when (this) {
    is Shape.Polygon -> points.centroid()
    is Shape.Ellipse -> center
}

/** 감싸는 사각형 (색종이 크기 계산, 라벨 표시 여부 판단) */
fun Shape.bounds(): Rect = when (this) {
    is Shape.Polygon -> Rect(
        left = points.minOf { it.x },
        top = points.minOf { it.y },
        right = points.maxOf { it.x },
        bottom = points.maxOf { it.y },
    )
    is Shape.Ellipse -> Rect(
        left = center.x - rx,
        top = center.y - ry,
        right = center.x + rx,
        bottom = center.y + ry,
    )
}

private const val ELLIPSE_SEGMENTS = 72