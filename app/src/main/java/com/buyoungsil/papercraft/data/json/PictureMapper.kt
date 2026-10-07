package com.buyoungsil.papercraft.data.json

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.buyoungsil.papercraft.data.model.Catalog
import com.buyoungsil.papercraft.data.model.Chapter
import com.buyoungsil.papercraft.data.model.Picture
import com.buyoungsil.papercraft.data.model.Piece
import com.buyoungsil.papercraft.data.model.Shape

/**
 * DTO → 앱 모델 변환.
 * 잘못된 데이터는 어떤 그림의 어떤 조각인지 알 수 있게 메시지를 담아 예외로 던진다.
 * (에셋은 우리가 직접 만드는 파일이라, 틀리면 바로 터져서 알아채는 게 낫다)
 */

// ───────── index.json ───────── ★ 추가

fun IndexDto.toModel(): Catalog {
    require(chapters.isNotEmpty()) { "[index] 챕터가 하나도 없어요" }

    val dupChapter = chapters.groupBy { it.id }.filterValues { it.size > 1 }.keys
    require(dupChapter.isEmpty()) { "[index] 챕터 id 중복: $dupChapter" }

    val allIds = chapters.flatMap { it.pictures }
    val dupPicture = allIds.groupBy { it }.filterValues { it.size > 1 }.keys
    require(dupPicture.isEmpty()) { "[index] 그림 id가 여러 챕터에 중복: $dupPicture" }
    require(practice !in allIds) { "[index] 연습 그림($practice)이 챕터에도 들어 있어요" }

    return Catalog(
        practiceId = practice,
        chapters = chapters.map { it.toModel() }
    )
}

fun ChapterDto.toModel(): Chapter {
    require(pictures.isNotEmpty()) { "[chapter $id] 그림이 하나도 없어요" }
    return Chapter(
        id = id,
        name = name,
        theme = theme,
        pictureIds = pictures
    )
}

// ───────── 그림 1장 ─────────

fun PictureDto.toModel(): Picture {
    val dupPiece = pieces.groupBy { it.id }.filterValues { it.size > 1 }.keys   // ★ 추가
    require(dupPiece.isEmpty()) { "[$id] 조각 id 중복: $dupPiece" }               // ★ 추가
    require(pieces.isNotEmpty()) { "[$id] 조각이 하나도 없어요" }                  // ★ 추가

    return Picture(
        id = id,
        name = name,
        noun = noun,
        level = level,
        description = description,
        pieces = pieces.map { it.toModel(pictureId = id) },
        practice = practice
    )
}

fun PieceDto.toModel(pictureId: String): Piece {
    val where = "$pictureId/$id"
    return Piece(
        id = id,
        name = name,
        shape = shape.toModel(where),
        color = parseHexColor(color, where),
        adjBig = adjBig,
        adjSmall = adjSmall
    )
}

fun ShapeDto.toModel(where: String): Shape =
    when (type) {
        "polygon" -> {
            val pts = requireNotNull(points) { "[$where] polygon 에 points 가 없어요" }
            require(pts.size >= 3) { "[$where] polygon 점이 3개 미만이에요 (${pts.size})" }
            Shape.Polygon(
                pts.mapIndexed { i, p ->
                    require(p.size == 2) { "[$where] points[$i] 는 [x, y] 형태여야 해요" }
                    Offset(p[0], p[1])
                }
            )
        }

        "ellipse" -> {
            val x = requireNotNull(cx) { "[$where] ellipse 에 cx 가 없어요" }
            val y = requireNotNull(cy) { "[$where] ellipse 에 cy 가 없어요" }
            val a = requireNotNull(rx) { "[$where] ellipse 에 rx 가 없어요" }
            val b = requireNotNull(ry) { "[$where] ellipse 에 ry 가 없어요" }
            require(a > 0f && b > 0f) { "[$where] ellipse 반지름은 0보다 커야 해요" }
            Shape.Ellipse(center = Offset(x, y), rx = a, ry = b)
        }

        else -> throw IllegalArgumentException("[$where] 모르는 shape type: $type")
    }

/**
 * "#RRGGBB" 또는 "#AARRGGBB" → Compose Color.
 * android.graphics.Color.parseColor 를 쓰지 않는 이유:
 * 그건 안드로이드 런타임에서만 돌아서 src/test 의 JVM 단위 테스트에서 터진다.
 */
fun parseHexColor(hex: String, where: String = ""): Color {
    val body = hex.removePrefix("#")
    val value = body.toLongOrNull(16)
        ?: throw IllegalArgumentException("[$where] 색상 형식이 잘못됐어요: $hex")
    return when (body.length) {
        6 -> Color(0xFF000000L or value)
        8 -> Color(value)
        else -> throw IllegalArgumentException("[$where] 색상은 #RRGGBB 또는 #AARRGGBB 여야 해요: $hex")
    }
}