package com.buyoungsil.papercraft.data.model

/**
 * 챕터 1개. 그림 자체가 아니라 "어떤 그림들이 어떤 순서로 묶였는지"만 가진다.
 * 그림 본문(Picture)은 필요할 때 id 로 따로 불러온다.
 */
data class Chapter(
    val id: Int,
    val name: String,          // "네모 나라"
    val theme: String,         // "직선만"
    val pictureIds: List<String>
)

/**
 * index.json 전체 = 연습 그림 + 챕터 목록.
 */
data class Catalog(
    val practiceId: String,
    val chapters: List<Chapter>
) {
    /** 모든 챕터의 그림 id를 순서대로 (잠금 해제 순서 계산용) */
    val allPictureIds: List<String>
        get() = chapters.flatMap { it.pictureIds }

    /** 이 그림이 속한 챕터 (연습 그림이면 null) */
    fun chapterOf(pictureId: String): Chapter? =
        chapters.firstOrNull { pictureId in it.pictureIds }

    /** 순서상 다음 그림 (마지막이거나 연습 그림이면 null) */
    fun nextOf(pictureId: String): String? {
        val all = allPictureIds
        val i = all.indexOf(pictureId)
        return if (i >= 0 && i + 1 < all.size) all[i + 1] else null
    }
}