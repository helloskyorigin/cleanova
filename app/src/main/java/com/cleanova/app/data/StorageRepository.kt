package com.cleanova.app.data

import android.app.Application
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.net.Uri
import com.cleanova.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class StorageRepository(private val app: Application) : StorageReader {
    override suspend fun capacity(): Capacity = withContext(Dispatchers.IO) {
        val fs = StatFs(Environment.getDataDirectory().absolutePath)
        Capacity(fs.totalBytes, fs.availableBytes)
    }

    private suspend fun count(uri: Uri, grant: Grant): MediaTally {
        if (grant == Grant.NONE) return MediaTally(0, 0, 0, false)
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                try {
                    val tally = MediaAccumulator()
                    val cursor = app.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null, signal)
                        ?: error("Media provider unavailable")
                    cursor.use {
                        while (it.moveToNext()) {
                            if (!continuation.isActive) return@use
                            tally.record(if (it.isNull(0)) null else it.getLong(0))
                        }
                    }
                    if (continuation.isActive) continuation.resume(tally.snapshot())
                } catch (e: OperationCanceledException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        }
    }
    override suspend fun inventory(access: MediaAccess): MediaInventory {
        require(access.hasAny)
        val images = count(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, access.images)
        val videos = count(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, access.videos)
        return MediaInventory(
            images.count, videos.count, images.knownBytes, videos.knownBytes, access,
            images.unknownSizes, videos.unknownSizes, images.overflow || videos.overflow
        )
    }
}
