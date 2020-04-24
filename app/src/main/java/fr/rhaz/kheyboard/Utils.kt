package fr.rhaz.kheyboard

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun JSONFile(file: File) = if (file.exists()) JSONObject(file.readText()) else JSONObject();

fun json(file: File, action: JSONObject.() -> Unit) {
    val result = JSONFile(file).apply(action)
    if (!file.exists()) file.createNewFile()
    file.writeText(result.toString())
}

fun <T> tryOr(def: T, block: () -> T) = try {
    block()
} catch (e: Exception) {
    def
}

fun JSONArray.toList() = mutableListOf<JSONObject>().also {
    for (i in 0 until length()) it.add(getJSONObject(i))
}

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