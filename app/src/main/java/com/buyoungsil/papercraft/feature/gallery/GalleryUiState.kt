package com.buyoungsil.papercraft.feature.gallery

import com.buyoungsil.papercraft.data.model.Artwork
import com.buyoungsil.papercraft.data.model.Picture

/**
 * 내 작품(갤러리) 화면 상태.
 */
sealed interface GalleryUiState {

    data object Loading : GalleryUiState

    data class Ready(
        /** 지금 필터에 맞는 작품들 (최신순) */
        val cards: List<ArtworkCard>,
        /** 필터 칩: 작품이 1점 이상 있는 그림만 (id, 이름, 작품 수) */
        val filters: List<GalleryFilter>,
        /** 선택된 필터 (null = 전체) */
        val selectedPictureId: String?,
        /** 크게 보기 중인 작품 (null = 목록) */
        val opened: ArtworkCard?,
        /** 필터 없이 전체 작품 수 */
        val totalCount: Int
    ) : GalleryUiState {
        val isEmpty: Boolean get() = totalCount == 0
    }
}

/**
 * 작품 카드 1장.
 * 작품에는 색이 없으므로 원래 그림(Picture)을 같이 들고 다닌다 → 조각 id 로 색을 찾는다.
 */
data class ArtworkCard(
    val artwork: Artwork,
    val picture: Picture
) {
    val id: String get() = artwork.id
    val title: String get() = artwork.title
    val stars: Int get() = artwork.stars
    /** 붙인 조각 수 / 전체 조각 수 (안 붙인 조각이 있는 작품 표시용) */
    val pieceSummary: String get() = "${artwork.pieces.size}/${picture.pieces.size}조각"
    val complete: Boolean get() = artwork.pieces.size == picture.pieces.size
}

data class GalleryFilter(
    val pictureId: String,
    val name: String,
    val count: Int
)