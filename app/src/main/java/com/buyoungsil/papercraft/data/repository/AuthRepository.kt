package com.buyoungsil.papercraft.data.repository

import android.util.Log
import com.buyoungsil.papercraft.data.firebase.await
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 창구.
 * 지금은 익명 로그인만 한다 → 사용자는 아무것도 안 해도 uid 가 생기고, 기록이 서버에 쌓인다.
 *
 * 익명 uid 는 앱을 지우거나 데이터를 삭제하면 사라진다.
 * 나중에 구글 로그인을 붙일 때 "익명 계정에 구글 계정을 연결(linkWithCredential)"하면
 * 같은 uid 가 유지되어 기록이 그대로 이어진다. → linkGoogle() 자리만 만들어 둠.
 */
@Singleton
class AuthRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val mutex = Mutex()

    private val _uid = MutableStateFlow(auth.currentUser?.uid)
    /** 로그인된 uid. 아직 없으면 null */
    val uid: StateFlow<String?> = _uid.asStateFlow()

    init {
        // 로그인/로그아웃이 어디서 일어나든 uid 를 따라간다
        auth.addAuthStateListener { _uid.value = it.currentUser?.uid }
    }

    /** 이미 로그인돼 있으면 익명 사용자인지 (구글 연결 안내 띄울 때 사용) */
    val isAnonymous: Boolean get() = auth.currentUser?.isAnonymous ?: true

    /**
     * 로그인 보장. 이미 되어 있으면 바로 uid, 아니면 익명 로그인 후 uid.
     * 여러 곳에서 동시에 불러도 로그인은 한 번만 한다.
     *
     * 첫 실행 + 인터넷 없음 → 예외. 부르는 쪽에서 잡아서 "나중에 다시" 처리.
     * (한 번이라도 로그인했으면 이후엔 오프라인에서도 currentUser 가 남아 있다)
     */
    suspend fun ensureSignedIn(): String = mutex.withLock {
        auth.currentUser?.uid?.let { return it }

        val result = auth.signInAnonymously().await()
        val uid = checkNotNull(result.user?.uid) { "익명 로그인 결과에 uid 가 없어요" }
        Log.d(TAG, "익명 로그인: $uid")
        _uid.value = uid
        uid
    }

    // TODO(로그인 단계): 구글 로그인 → auth.currentUser!!.linkWithCredential(credential)
    // fun linkGoogle(idToken: String) { ... }

    private companion object {
        const val TAG = "AuthRepository"
    }
}