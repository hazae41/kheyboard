package fr.rhaz.kheyboard.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

typealias Catchable = (Throwable) -> Unit

fun job(block: (() -> Unit, Catchable) -> Unit): Job {
    val job = Job()

    fun resolve() {
        job.complete()
    }

    fun reject(error: Throwable) {
        job.completeExceptionally(error)
    }

    block(::resolve, ::reject)
    return job
}

fun <T> deferred(block: ((T) -> Unit, Catchable) -> Unit): Deferred<T> {
    val deferred = CompletableDeferred<T>()

    fun resolve(value: T) {
        deferred.complete(value)
    }

    fun reject(error: Throwable) {
        deferred.completeExceptionally(error)
    }

    block(::resolve, ::reject)
    return deferred
}
