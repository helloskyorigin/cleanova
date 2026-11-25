package com.cleanova.app.data

import android.app.Application
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class Capacity(val total: Long, val available: Long) {
    val used: Long get() = (total - available).coerceAtLeast(0)
    val fraction: Float get() = if (total > 0) (used.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
}
data class MediaInventory(val photos: Long, val videos: Long, val photoBytes: Long, val videoBytes: Long)
class StorageRepository(private val app: Application) {
    suspend fun capacity(): Capacity = withContext(Dispatchers.IO) {
        val fs = StatFs(Environment.getDataDirectory().absolutePath)
        Capacity(fs.totalBytes, fs.availableBytes)
    }
    suspend fun inventory(): MediaInventory = withContext(Dispatchers.IO) {
        suspend fun count(uri: android.net.Uri): Pair<Long, Long> {
            var count = 0L
            var bytes = 0L
            val cursor = app.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)
                ?: error("Media provider unavailable")
            cursor.use {
                while (it.moveToNext()) {
                    coroutineContext.ensureActive()
                    count++
                    bytes += it.getLong(0).coerceAtLeast(0)
                }
            }
            return count to bytes
        }
        val images = count(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val videos = count(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        MediaInventory(images.first, videos.first, images.second, videos.second)
    }
}
