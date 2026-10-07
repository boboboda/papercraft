package com.buyoungsil.papercraft.data.repository

import android.util.Log
import com.buyoungsil.papercraft.data.model.Artwork
import com.buyoungsil.papercraft.data.model.ArtworkPiece
import com.buyoungsil.papercraft.data.model.simplify
import com.buyoungsil.papercraft.data.model.toFlat
import com.buyoungsil.papercraft.data.model.toOffsets
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 내 작품 저장소.
 *
 *   users/{uid}/artworks/{자동 id}
 *     pictureId: "cat"
 *     title: "귀가 없는 고양이"
 *     stars: 2, avgCells: 0.84, createdAt: 1759...
 *     pieces: [ { pieceId: "body", points: [x,y,x,y,...] }, ... ]
 *
 * (배열 안에 "맵"을 넣고 그 맵 안에 배열을 넣는 건 Firestore 가 허용한다)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ArtworkRepository @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 최신순 작품 목록 (최대 MAX 개) */
    val artworks: StateFlow<List<Artwork>> = authRepository.uid
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else observe(uid) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ───────────────────────── 읽기 ─────────────────────────

    private fun observe(uid: String) = callbackFlow {
        val registration = collection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(MAX)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "작품 불러오기 실패", error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { parse(it) }.orEmpty())
            }
        awaitClose { registration.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parse(doc: DocumentSnapshot): Artwork? {
        return try {
            val pictureId = doc.getString("pictureId") ?: return null
            val pieces = (doc.get("pieces") as? List<Map<String, Any?>>).orEmpty().mapNotNull { m ->
                val id = m["pieceId"] as? String ?: return@mapNotNull null
                val pts = (m["points"] as? List<Number>).orEmpty().toOffsets()
                if (pts.size < 3) null else ArtworkPiece(id, pts)
            }
            Artwork(
                id = doc.id,
                pictureId = pictureId,
                title = doc.getString("title").orEmpty(),
                stars = doc.getLong("stars")?.toInt() ?: 0,
                avgCells = doc.getDouble("avgCells")?.toFloat() ?: 0f,
                createdAt = doc.getLong("createdAt") ?: 0L,
                pieces = pieces
            )
        } catch (e: Exception) {
            Log.w(TAG, "작품 형식 오류: ${doc.id}", e)
            null
        }
    }

    // ───────────────────────── 쓰기 ─────────────────────────

    /**
     * 작품 저장. 기다리지 않는다(오프라인 대비).
     * @return 새 문서 id (로그인 안 됐으면 null)
     */
    fun save(artwork: Artwork): String? {
        val uid = authRepository.uid.value ?: run {
            Log.w(TAG, "로그인 안 됨 → 작품 저장 못 함")
            return null
        }
        val ref = collection(uid).document()          // id 를 먼저 만들어 둔다
        val data = mapOf(
            "pictureId" to artwork.pictureId,
            "title" to artwork.title,
            "stars" to artwork.stars,
            "avgCells" to artwork.avgCells.toDouble(),
            "createdAt" to artwork.createdAt,
            "pieces" to artwork.pieces.map { p ->
                mapOf(
                    "pieceId" to p.pieceId,
                    "points" to p.points.simplify().toFlat()
                )
            }
        )
        ref.set(data).addOnFailureListener { Log.w(TAG, "작품 저장 실패", it) }
        return ref.id
    }

    fun delete(artworkId: String) {
        val uid = authRepository.uid.value ?: return
        collection(uid).document(artworkId).delete()
            .addOnFailureListener { Log.w(TAG, "작품 삭제 실패: $artworkId", it) }
    }

    private fun collection(uid: String) =
        db.collection("users").document(uid).collection("artworks")

    private companion object {
        const val TAG = "ArtworkRepository"
        const val MAX = 200L
    }
}