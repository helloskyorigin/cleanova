package com.cleanova.app.domain

data class Capacity(val total: Long, val available: Long) {
    val used: Long get() = total.coerceAtLeast(0) - available.coerceIn(0, total.coerceAtLeast(0))
    val fraction: Float get() = if (total > 0) (used.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
}

/** Grant per media type, not a claim that the entire gallery is visible. */
enum class Grant { NONE, SELECTED, FULL }
data class MediaAccess(val images: Grant, val videos: Grant) {
    val hasAny: Boolean get() = images != Grant.NONE || videos != Grant.NONE
    val isFull: Boolean get() = images == Grant.FULL && videos == Grant.FULL
    val label: String get() = if (isFull) "Full" else if (hasAny) "Limited" else "None"
}

data class MediaInventory(
    val photos: Long, val videos: Long, val photoBytes: Long, val videoBytes: Long,
    val access: MediaAccess, val unknownPhotoSizes: Long = 0, val unknownVideoSizes: Long = 0,
    val sizeOverflow: Boolean = false
) {
    val incompleteSize: Boolean get() = unknownPhotoSizes > 0 || unknownVideoSizes > 0 || sizeOverflow
}

interface StorageReader {
    suspend fun capacity(): Capacity
    suspend fun inventory(access: MediaAccess): MediaInventory
}
