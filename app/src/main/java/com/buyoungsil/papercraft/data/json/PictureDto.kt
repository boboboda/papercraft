package com.buyoungsil.papercraft.data.json

import com.squareup.moshi.JsonClass

/**
 * assets/pictures/ 의 JSON을 그대로 받는 그릇들.
 * 화면·게임 로직은 이 클래스를 직접 쓰지 않고,
 * PictureMapper 가 Picture/Piece/Shape 로 바꿔서 넘겨준다.
 */

// ───────── index.json ─────────

@JsonClass(generateAdapter = true)
data class IndexDto(
    val practice: String,
    val chapters: List<ChapterDto>
)

@JsonClass(generateAdapter = true)
data class ChapterDto(
    val id: Int,
    val name: String,
    val theme: String,
    val pictures: List<String>
)

// ───────── 그림 1장 (house.json 등) ─────────

@JsonClass(generateAdapter = true)
data class PictureDto(
    val id: String,
    val name: String,
    val noun: String,
    val level: Int,
    val practice: Boolean = false,
    val description: String,
    val pieces: List<PieceDto>
)

@JsonClass(generateAdapter = true)
data class PieceDto(
    val id: String,
    val name: String,
    val color: String,          // "#RRGGBB"
    val adjBig: String,
    val adjSmall: String,
    val shape: ShapeDto
)

/**
 * shape 는 type 에 따라 쓰는 필드가 다르다.
 *  - "polygon" → points
 *  - "ellipse" → cx, cy, rx, ry
 * 다형성 어댑터 대신 필드를 전부 nullable 로 두고 매퍼에서 검사한다. (단순한 방법)
 */
@JsonClass(generateAdapter = true)
data class ShapeDto(
    val type: String,
    val points: List<List<Float>>? = null,
    val cx: Float? = null,
    val cy: Float? = null,
    val rx: Float? = null,
    val ry: Float? = null
)