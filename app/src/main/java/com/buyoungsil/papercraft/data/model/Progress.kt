package com.buyoungsil.papercraft.data.model

/**
 * 그림 1장의 최고 기록.
 * "최고" 기준: 별이 많은 쪽 → 별이 같으면 평균 오차가 작은 쪽.
 */
data class PictureProgress(
    val pictureId: String,
    val bestStars: Int = 0,               // 0..3
    val bestAvgCells: Float? = null,      // 평균 오차(칸). 한 번도 안 했으면 null
    val bestTitle: String? = null,        // 최고 기록 때의 제목 〈...〉
    val plays: Int = 0,                   // 끝까지 한 횟수
    val updatedAt: Long = 0L              // epoch millis
) {
    val cleared: Boolean get() = bestStars > 0

    /** 새 결과를 반영한 기록 (더 좋을 때만 best 갱신, plays 는 항상 +1) */
    fun merge(stars: Int, avgCells: Float, title: String, now: Long): PictureProgress {
        val better = stars > bestStars ||
                (stars == bestStars && (bestAvgCells == null || avgCells < bestAvgCells))
        return if (better) {
            copy(bestStars = stars, bestAvgCells = avgCells, bestTitle = title, plays = plays + 1, updatedAt = now)
        } else {
            copy(plays = plays + 1, updatedAt = now)
        }
    }
}

/**
 * 사용자 진행 상황 전체.
 */
data class Progress(
    val pictures: Map<String, PictureProgress> = emptyMap(),
    /** 연습 제외, 끝까지 완성한 총 횟수 → 전면 광고 주기 계산용 */
    val completedCount: Int = 0,
    /** "광고 제거" 구매 여부 (전면 광고만 제거) */
    val removeAds: Boolean = false
) {
    fun of(pictureId: String): PictureProgress =
        pictures[pictureId] ?: PictureProgress(pictureId)

    fun starsOf(pictureId: String): Int = pictures[pictureId]?.bestStars ?: 0

    val totalStars: Int get() = pictures.values.sumOf { it.bestStars }

    /**
     * 잠금 규칙: 전체 순서에서 바로 앞 그림을 깼으면 열림.
     * - 각 챕터의 첫 그림은, 앞 챕터를 절반 이상 깼으면 열림 (한 그림에 막혀서 진행이 멈추지 않게)
     * - 맨 첫 그림은 항상 열림
     */
    fun isUnlocked(catalog: Catalog, pictureId: String): Boolean {
        val all = catalog.allPictureIds
        val index = all.indexOf(pictureId)
        if (index <= 0) return true                             // 첫 그림 / 목록에 없는 그림(연습)
        if (of(all[index - 1]).cleared) return true

        val chapterIndex = catalog.chapters.indexOfFirst { pictureId in it.pictureIds }
        val chapter = catalog.chapters.getOrNull(chapterIndex) ?: return false
        if (chapter.pictureIds.first() == pictureId && chapterIndex > 0) {
            val prev = catalog.chapters[chapterIndex - 1]
            val clearedInPrev = prev.pictureIds.count { of(it).cleared }
            return clearedInPrev * 2 >= prev.pictureIds.size
        }
        return false
    }
}