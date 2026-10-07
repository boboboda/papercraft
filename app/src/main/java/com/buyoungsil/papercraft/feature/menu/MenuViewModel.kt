package com.buyoungsil.papercraft.feature.menu

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buyoungsil.papercraft.data.model.Catalog
import com.buyoungsil.papercraft.data.model.Picture
import com.buyoungsil.papercraft.data.model.Progress
import com.buyoungsil.papercraft.data.repository.PictureRepository
import com.buyoungsil.papercraft.data.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 메뉴 = (그림 목록: 한 번만 불러옴) + (진행 상황: 실시간)
 * 둘을 합쳐서 MenuUiState 를 만든다.
 * → 그림을 깨고 돌아오면 별과 잠금 해제가 바로 반영된다.
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val pictureRepository: PictureRepository,
    progressRepository: ProgressRepository
) : ViewModel() {

    /** 에셋에서 읽은 그림 목록 (진행과 무관, 바뀌지 않음) */
    private sealed interface Content {
        data object Loading : Content
        data class Error(val message: String) : Content
        data class Ok(
            val catalog: Catalog,
            val practice: Picture,
            val chapters: List<Pair<com.buyoungsil.papercraft.data.model.Chapter, List<Picture>>>
        ) : Content
    }

    private val content = MutableStateFlow<Content>(Content.Loading)

    val uiState: StateFlow<MenuUiState> =
        combine(content, progressRepository.progress) { c, progress -> toUiState(c, progress) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),   // 화면 회전 동안 끊기지 않게 5초 여유
                initialValue = MenuUiState.Loading
            )

    init {
        load()
    }

    /** 에러 화면의 "다시 시도" 버튼 */
    fun retry() {
        load()
    }

    private fun load() {
        content.value = Content.Loading
        viewModelScope.launch {
            content.value = try {
                val catalog = pictureRepository.catalog()
                Content.Ok(
                    catalog = catalog,
                    practice = pictureRepository.practice(),
                    chapters = catalog.chapters.map { ch -> ch to pictureRepository.pictures(ch.pictureIds) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "메뉴 불러오기 실패", e)
                Content.Error(e.message ?: "그림을 불러오지 못했어요")
            }
        }
    }

    private fun toUiState(c: Content, progress: Progress): MenuUiState = when (c) {
        Content.Loading -> MenuUiState.Loading
        is Content.Error -> MenuUiState.Error(c.message)
        is Content.Ok -> MenuUiState.Ready(
            practice = PictureCard(picture = c.practice),
            chapters = c.chapters.map { (chapter, pictures) ->
                ChapterSection(
                    id = chapter.id,
                    name = chapter.name,
                    theme = chapter.theme,
                    cards = pictures.map { pic ->
                        PictureCard(
                            picture = pic,
                            stars = progress.starsOf(pic.id),
                            locked = !progress.isUnlocked(c.catalog, pic.id)
                        )
                    }
                )
            }
        )
    }

    private companion object {
        const val TAG = "MenuViewModel"
    }
}