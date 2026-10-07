package com.buyoungsil.papercraftlab.feature.gallery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buyoungsil.papercraftlab.data.repository.ArtworkRepository
import com.buyoungsil.papercraftlab.data.repository.PictureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 갤러리 = (작품 목록: Firestore 실시간) + (그림 정보: 에셋) + (필터·크게 보기: 화면 조작)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val artworkRepository: ArtworkRepository,
    private val pictureRepository: PictureRepository
) : ViewModel() {

    private val selectedPictureId = MutableStateFlow<String?>(null)
    private val openedId = MutableStateFlow<String?>(null)

    /** 작품마다 원래 그림을 붙인 카드 목록 (그림은 저장소가 캐시하므로 빠름) */
    private val cards = artworkRepository.artworks.mapLatest { artworks ->
        artworks.mapNotNull { art ->
            try {
                ArtworkCard(artwork = art, picture = pictureRepository.picture(art.pictureId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 그림 파일이 없어진 경우(나중에 그림을 빼거나 id 를 바꿨을 때) → 그 작품만 숨김
                Log.w(TAG, "그림 없음: ${art.pictureId}", e)
                null
            }
        }
    }

    val uiState: StateFlow<GalleryUiState> =
        combine(cards, selectedPictureId, openedId) { all, filter, opened ->
            val filters = all
                .groupBy { it.picture.id }
                .map { (id, list) -> GalleryFilter(id, list.first().picture.name, list.size) }
                .sortedByDescending { it.count }

            // 고른 필터의 작품이 다 지워졌으면 전체로 되돌림
            val effectiveFilter = filter?.takeIf { f -> filters.any { it.pictureId == f } }

            GalleryUiState.Ready(
                cards = if (effectiveFilter == null) all else all.filter { it.picture.id == effectiveFilter },
                filters = filters,
                selectedPictureId = effectiveFilter,
                opened = opened?.let { id -> all.firstOrNull { it.id == id } },
                totalCount = all.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GalleryUiState.Loading
        )

    // ───────────────────────── 화면 조작 ─────────────────────────

    /** 필터 칩 누르기 (같은 칩을 다시 누르면 전체) */
    fun selectFilter(pictureId: String?) {
        selectedPictureId.value = if (selectedPictureId.value == pictureId) null else pictureId
    }

    fun open(artworkId: String) {
        openedId.value = artworkId
    }

    fun close() {
        openedId.value = null
    }

    fun delete(artworkId: String) {
        artworkRepository.delete(artworkId)
        if (openedId.value == artworkId) openedId.value = null
    }

    private companion object {
        const val TAG = "GalleryViewModel"
    }
}