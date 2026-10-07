package com.buyoungsil.papercraft.data.firebase

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Task 를 코루틴에서 기다리기.
 *
 *   val user = auth.signInAnonymously().await().user
 *
 * kotlinx-coroutines-play-services 라이브러리의 await() 와 같은 역할.
 * 라이브러리를 하나 더 넣지 않으려고 직접 만든다.
 *
 * ⚠️ Firestore "쓰기"(set/update)에는 쓰지 말 것.
 *    오프라인이면 서버가 받아 줄 때까지 Task 가 끝나지 않아서 영원히 기다리게 된다.
 *    쓰기는 로컬 캐시에 즉시 반영되므로 기다리지 않고 던져 두면 된다 (fire-and-forget).
 */
suspend fun <T> Task<T>.await(): T {
    // 이미 끝난 Task 면 바로 결과
    if (isComplete) {
        val e = exception
        return when {
            e != null -> throw e
            isCanceled -> throw CancellationException("Task $this was cancelled")
            else -> @Suppress("UNCHECKED_CAST") (result as T)
        }
    }

    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            val e = task.exception
            when {
                e != null -> cont.resumeWithException(e)
                task.isCanceled -> cont.cancel()
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    cont.resume(task.result as T)
                }
            }
        }
    }
}