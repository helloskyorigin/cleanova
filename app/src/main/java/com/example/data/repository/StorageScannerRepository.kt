package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.model.AuditedMediaItem
import com.example.data.model.DeviceStorageStats
import com.example.data.model.FolderFileCategory
import com.example.data.model.MediaAuditCategory
import com.example.data.model.ScannedFolderFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

class StorageScannerRepository(private val context: Context) {

    suspend fun readDeviceStorageStats(): DeviceStorageStats = withContext(Dispatchers.IO) {
        val dataDir = Environment.getDataDirectory()
        val statFs = StatFs(dataDir.path)
        val total = statFs.totalBytes
        val free = statFs.availableBytes
        val used = (total - free).coerceAtLeast(0L)

        val cacheBytes = directorySize(context.cacheDir) +
            directorySize(context.codeCacheDir) +
            (context.externalCacheDir?.let { directorySize(it) } ?: 0L)
        val workspaceBytes = directorySize(context.filesDir)

        val freeFraction = if (total > 0L) free.toFloat() / total.toFloat() else 0.5f
        val baseScore = (freeFraction * 100f).toInt().coerceIn(15, 95)
        val cachePenalty = (cacheBytes / (10L * 1024L * 1024L)).toInt().coerceIn(0, 12)
        val healthScore = (baseScore + 18 - cachePenalty).coerceIn(24, 99)

        DeviceStorageStats(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free,
            appCacheBytes = cacheBytes,
            appWorkspaceBytes = workspaceBytes,
            healthScore = healthScore
        )
    }

    suspend fun clearAppCache(): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        val dirs = listOfNotNull(
            context.cacheDir,
            context.codeCacheDir,
            context.externalCacheDir
        )
        for (dir in dirs) {
            freed += deleteChildrenRecursively(dir)
        }
        freed
    }

    suspend fun inspectPickedMedia(uris: List<Uri>): List<AuditedMediaItem> = withContext(Dispatchers.IO) {
        val rawItems = uris.mapNotNull { uri ->
            readMediaMetadata(uri)
        }

        // Detect exact-size or normalized-name duplicate candidates
        val sizeGroups = rawItems.groupBy { "${it.sizeBytes}_${it.isVideo}" }
        val nameGroups = rawItems.groupBy { normalizeBaseName(it.displayName) }

        rawItems.map { item ->
            val hasSizeMatch = item.sizeBytes > 0L && (sizeGroups["${item.sizeBytes}_${item.isVideo}"]?.size ?: 0) > 1
            val hasNameMatch = (nameGroups[normalizeBaseName(item.displayName)]?.size ?: 0) > 1

            val (category, reason, defaultSelect) = when {
                hasSizeMatch || hasNameMatch -> Triple(
                    MediaAuditCategory.DUPLICATE_CANDIDATE,
                    if (hasSizeMatch) "Identical file size match (${formatBytes(item.sizeBytes)})"
                    else "Similar filename match",
                    true
                )
                item.isVideo && item.sizeBytes >= 20L * 1024L * 1024L -> Triple(
                    MediaAuditCategory.LARGE_VIDEO,
                    "Large video (${formatBytes(item.sizeBytes)})",
                    true
                )
                item.displayName.lowercase(Locale.US).contains("screenshot") ||
                    (item.width > 0 && item.height > 0 && item.width * item.height <= 2_100_000) -> Triple(
                    MediaAuditCategory.SCREENSHOT_OR_COMPACT,
                    "Screenshot or compact capture",
                    false
                )
                item.sizeBytes >= 4L * 1024L * 1024L -> Triple(
                    MediaAuditCategory.HEAVY_PHOTO,
                    "High-resolution photo (${formatBytes(item.sizeBytes)})",
                    false
                )
                else -> Triple(
                    MediaAuditCategory.HEAVY_PHOTO,
                    "${formatBytes(item.sizeBytes)} media item",
                    false
                )
            }

            item.copy(
                category = category,
                reason = reason,
                isSelected = defaultSelect
            )
        }.sortedByDescending { it.sizeBytes }
    }

    suspend fun scanFolderTree(treeUri: Uri): Pair<String, List<ScannedFolderFile>> = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
        val folderName = queryDisplayName(rootUri) ?: "Selected Folder"

        val collected = mutableListOf<ScannedFolderFile>()
        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add(rootDocId to 0)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        while (queue.isNotEmpty() && collected.size < 400) {
            val (parentDocId, depth) = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext() && collected.size < 400) {
                    val docId = if (idCol >= 0) cursor.getString(idCol) else continue
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Unnamed" else "Unnamed"
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                    val size = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                    val modified = if (modCol >= 0 && !cursor.isNull(modCol)) cursor.getLong(modCol) else 0L

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (depth < 3 && !name.startsWith(".")) {
                            queue.add(docId to depth + 1)
                        }
                    } else {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        collected.add(
                            ScannedFolderFile(
                                uriString = docUri.toString(),
                                documentId = docId,
                                name = name,
                                sizeBytes = size,
                                mimeType = mime,
                                lastModifiedMs = modified,
                                category = FolderFileCategory.GENERAL,
                                hint = formatBytes(size),
                                isSelected = false
                            )
                        )
                    }
                }
            }
        }

        // Classify duplicates, APKs/archives, and large files
        val sizeGroups = collected.filter { it.sizeBytes > 0L }.groupBy { it.sizeBytes }
        val classified = collected.map { file ->
            val lower = file.name.lowercase(Locale.US)
            val isDup = file.sizeBytes > 0L && (sizeGroups[file.sizeBytes]?.size ?: 0) > 1
            val isArchive = lower.endsWith(".apk") || lower.endsWith(".zip") ||
                lower.endsWith(".rar") || lower.endsWith(".7z") || lower.endsWith(".tar.gz")
            val isLarge = file.sizeBytes >= 10L * 1024L * 1024L

            when {
                isDup -> file.copy(
                    category = FolderFileCategory.DUPLICATE_FILE,
                    hint = "Exact size match (${formatBytes(file.sizeBytes)})",
                    isSelected = true
                )
                isArchive -> file.copy(
                    category = FolderFileCategory.APK_OR_ARCHIVE,
                    hint = "Installer or archive • ${formatBytes(file.sizeBytes)}",
                    isSelected = true
                )
                isLarge -> file.copy(
                    category = FolderFileCategory.LARGE_FILE,
                    hint = "Large file • ${formatBytes(file.sizeBytes)}",
                    isSelected = false
                )
                else -> file.copy(
                    category = FolderFileCategory.GENERAL,
                    hint = formatBytes(file.sizeBytes),
                    isSelected = false
                )
            }
        }.sortedByDescending { it.sizeBytes }

        folderName to classified
    }

    suspend fun deleteFolderFiles(files: List<ScannedFolderFile>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var deletedCount = 0
        var freedBytes = 0L
        for (file in files) {
            try {
                val deleted = DocumentsContract.deleteDocument(
                    context.contentResolver,
                    Uri.parse(file.uriString)
                )
                if (deleted) {
                    deletedCount++
                    freedBytes += file.sizeBytes
                }
            } catch (_: Exception) {
                // Skip protected or already-removed documents cleanly
            }
        }
        deletedCount to freedBytes
    }

    private fun readMediaMetadata(uri: Uri): AuditedMediaItem? {
        var name = "Media Item"
        var size = 0L
        var width = 0
        var height = 0
        val mime = context.contentResolver.getType(uri) ?: "image/*"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                val wIdx = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val hIdx = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                if (wIdx >= 0 && !cursor.isNull(wIdx)) width = cursor.getInt(wIdx)
                if (hIdx >= 0 && !cursor.isNull(hIdx)) height = cursor.getInt(hIdx)
            }
        }

        return AuditedMediaItem(
            uriString = uri.toString(),
            displayName = name,
            sizeBytes = size,
            mimeType = mime,
            width = width,
            height = height,
            isVideo = mime.startsWith("video/"),
            category = MediaAuditCategory.HEAVY_PHOTO,
            reason = formatBytes(size),
            isSelected = false
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeBaseName(name: String): String {
        val noExt = name.substringBeforeLast('.')
        return noExt
            .replace(Regex("""\(\d+\)$"""), "")
            .replace(Regex("""[-_\s](copy|dup|\d+)$""", RegexOption.IGNORE_CASE), "")
            .trim()
            .lowercase(Locale.US)
    }

    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            total += if (file.isDirectory) directorySize(file) else file.length()
        }
        return total
    }

    private fun deleteChildrenRecursively(dir: File): Long {
        if (!dir.exists()) return 0L
        var freed = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            freed += if (child.isDirectory) {
                val subFreed = deleteChildrenRecursively(child)
                child.delete()
                subFreed
            } else {
                val len = child.length()
                if (child.delete()) len else 0L
            }
        }
        return freed
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
                .coerceIn(0, units.lastIndex)
            val value = bytes / 1024.0.pow(digitGroups.toDouble())
            return if (digitGroups >= 2) {
                String.format(Locale.US, "%.1f %s", value, units[digitGroups])
            } else {
                String.format(Locale.US, "%.0f %s", value, units[digitGroups])
            }
        }
    }
}
