package com.buyoungsil.papercraftlab.data.model

import androidx.compose.ui.graphics.Color

/**
 * 밑그림의 조각 하나 (예: 고양이의 "왼쪽 귀")
 *
 * @param id 그림 안에서 고유한 id ("earL"). 저장·판정 결과를 이 id로 연결
 * @param name 화면에 보이는 이름 ("왼쪽 귀")
 * @param shape 목표 모양 (밑그림 좌표)
 * @param color 색종이 색
 * @param adjBig 너무 크게 잘랐을 때 완성작 제목에 쓰는 말 ("귀가 쫑긋한")
 * @param adjSmall 너무 작게 잘랐을 때 쓰는 말 ("귀가 작은")
 */
data class Piece(
    val id: String,
    val name: String,
    val shape: Shape,
    val color: Color,
    val adjBig: String,
    val adjSmall: String,
)