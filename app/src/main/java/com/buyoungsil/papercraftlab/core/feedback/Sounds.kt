package com.buyoungsil.papercraftlab.core.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 효과음 모음. SoundPool = 짧은 소리를 지연 없이 겹쳐 재생하는 용도.
 *
 * 파일은 res/raw/ 에 아래 이름으로 넣는다 (확장자 .ogg 권장, .mp3/.wav 도 됨):
 *   snip.ogg     가위 "싹" (자르는 동안 반복)
 *   split.ogg    종이 갈라지는 소리
 *   paste.ogg    조각 붙이는 "톡"
 *   click.ogg    버튼
 *   success.ogg  완성 / 별
 *
 * 파일이 없어도 빌드·실행은 된다 → 없는 소리만 조용히 건너뜀.
 * (R.raw.snip 처럼 직접 쓰면 파일이 없을 때 빌드가 깨지므로 이름으로 찾는다)
 */
@Singleton
class Sounds @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class Sfx(val fileName: String, val volume: Float) {
        SNIP("snip", 0.55f),
        SPLIT("split", 0.9f),
        PASTE("paste", 0.8f),
        CLICK("click", 0.5f),
        SUCCESS("success", 1.0f)
    }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    /** Sfx → SoundPool 안의 번호 (파일 없으면 목록에 없음) */
    private val loaded = mutableMapOf<Sfx, Int>()
    /** 메모리에 올라가서 바로 재생 가능한 번호들 */
    private val ready = mutableSetOf<Int>()

    /** 설정 화면에서 끌 수 있게 (설정 저장은 나중 단계) */
    var enabled: Boolean = true

    private var lastSnipAt = 0L

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) ready += sampleId
        }
        Sfx.entries.forEach { sfx ->
            val resId = rawId(sfx.fileName)
            if (resId != 0) {
                loaded[sfx] = pool.load(context, resId, 1)
            } else {
                Log.d(TAG, "효과음 없음 (건너뜀): res/raw/${sfx.fileName}")
            }
        }
    }

    fun play(sfx: Sfx, rate: Float = 1f) {
        if (!enabled) return
        val id = loaded[sfx] ?: return
        if (id !in ready) return                 // 아직 로딩 중이면 이번만 건너뜀
        pool.play(id, sfx.volume, sfx.volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /**
     * 자르는 중 매 프레임 불러도 됨. SNIP_MS 간격으로만 재생.
     * 소리 높이를 조금씩 흔들어서 기계적으로 반복되는 느낌을 줄인다.
     */
    fun snip() {
        val now = SystemClock.uptimeMillis()
        if (now - lastSnipAt < SNIP_MS) return
        lastSnipAt = now
        play(Sfx.SNIP, rate = 0.92f + Math.random().toFloat() * 0.16f)
    }

    @SuppressLint("DiscouragedApi")   // 파일이 없어도 빌드되게 하려고 일부러 이름으로 찾음
    private fun rawId(name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    private companion object {
        const val TAG = "Sounds"
        const val SNIP_MS = 180L
    }
}