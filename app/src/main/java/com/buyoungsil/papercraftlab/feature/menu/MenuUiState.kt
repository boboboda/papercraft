package com.buyoungsil.papercraftlab.feature.menu

import com.buyoungsil.papercraftlab.data.model.Picture

/**
 * 메뉴 화면이 그리는 데 필요한 모든 것.
 * 화면은 이 상태만 보고 그리며, 데이터를 직접 불러오지 않는다.
 */
sealed interface MenuUiState {

    data object Loading : MenuUiState

    data class Error(val message: String) : MenuUiState

    data class Ready(
        val practice: PictureCard,
        val chapters: List<ChapterSection>
    ) : MenuUiState {
        /** 전체 별 개수 / 최대 별 개수 (상단 표시용) */
        val totalStars: Int get() = chapters.sumOf { c -> c.cards.sumOf { it.stars } }
        val maxStars: Int get() = chapters.sumOf { it.cards.size } * 3
    }
}

/** 챕터 한 묶음 (가로 줄 하나) */
data class ChapterSection(
    val id: Int,
    val name: String,              // "네모 나라"
    val theme: String,             // "직선만"
    val cards: List<PictureCard>
) {
    val clearedCount: Int get() = cards.count { it.stars > 0 }
    /** 챕터 안의 그림이 전부 잠겨 있으면 챕터 자체를 흐리게 */
    val locked: Boolean get() = cards.all { it.locked }
}

/** 그림 카드 1장 */
data class PictureCard(
    val picture: Picture,          // 썸네일을 그리려면 조각 모양이 필요
    val stars: Int = 0,            // 0 = 아직 안 깸, 1~3
    val locked: Boolean = false
) {
    val id: String get() = picture.id
    val name: String get() = picture.name
    val pieceCount: Int get() = picture.pieces.size
}