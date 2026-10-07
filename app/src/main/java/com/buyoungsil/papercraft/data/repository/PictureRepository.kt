package com.buyoungsil.papercraft.data.repository

import android.content.Context
import com.buyoungsil.papercraft.data.json.IndexDto
import com.buyoungsil.papercraft.data.json.PictureDto
import com.buyoungsil.papercraft.data.json.toModel
import com.buyoungsil.papercraft.data.model.Catalog
import com.buyoungsil.papercraft.data.model.Picture
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 그림 데이터 창구.
 * 지금은 assets/pictures/ 에서 읽지만, 나중에 서버에서 새 그림을 받게 되어도
 * 화면/뷰모델은 이 클래스만 보므로 바깥 코드는 바뀌지 않는다.
 *
 * - 앱 전체에 하나 (@Singleton) → 한 번 읽은 그림은 메모리에 남겨 두고 재사용
 * - 파일 읽기는 IO 스레드에서
 */
@Singleton
class PictureRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // KSP 코드 생성 어댑터를 쓰므로 KotlinJsonAdapterFactory(리플렉션) 불필요
    private val moshi: Moshi = Moshi.Builder().build()
    private val indexAdapter = moshi.adapter(IndexDto::class.java)
    private val pictureAdapter = moshi.adapter(PictureDto::class.java)

    private val mutex = Mutex()
    private var catalogCache: Catalog? = null
    private val pictureCache = mutableMapOf<String, Picture>()

    /** index.json → 챕터 목록 */
    suspend fun catalog(): Catalog = mutex.withLock {
        catalogCache ?: run {
            val dto = readJson("$DIR/index.json") { indexAdapter.fromJson(it) }
            dto.toModel().also { catalogCache = it }
        }
    }

    /** 그림 1장 */
    suspend fun picture(id: String): Picture = mutex.withLock {
        pictureCache[id] ?: run {
            val dto = readJson("$DIR/$id.json") { pictureAdapter.fromJson(it) }
            require(dto.id == id) { "[$id.json] 파일 이름과 안의 id(${dto.id})가 달라요" }
            dto.toModel().also { pictureCache[id] = it }
        }
    }

    /** 여러 장 (메뉴 썸네일용). 순서는 ids 순서 그대로 */
    suspend fun pictures(ids: List<String>): List<Picture> =
        ids.map { picture(it) }

    /** 연습 그림 */
    suspend fun practice(): Picture = picture(catalog().practiceId)

    // ───────── 내부 ─────────

    private suspend fun <T : Any> readJson(path: String, parse: (String) -> T?): T =
        withContext(Dispatchers.IO) {
            val text = try {
                context.assets.open(path).bufferedReader().use { it.readText() }
            } catch (e: FileNotFoundException) {
                throw IllegalStateException("에셋 파일이 없어요: $path", e)
            }
            parse(text) ?: throw IllegalStateException("JSON 이 비어 있어요: $path")
        }

    private companion object {
        const val DIR = "pictures"
    }
}