package com.example.data.model

data class DeviceStorageStats(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val appCacheBytes: Long = 0L,
    val appWorkspaceBytes: Long = 0L,
    val healthScore: Int = 88
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

enum class MediaAuditCategory(val label: String) {
    ALL("All"),
    DUPLICATE_CANDIDATE("Duplicates"),
    LARGE_VIDEO("Large Videos"),
    HEAVY_PHOTO("Heavy Photos"),
    SCREENSHOT_OR_COMPACT("Compact / Screenshots")
}

data class AuditedMediaItem(
    val uriString: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val isVideo: Boolean,
    val category: MediaAuditCategory,
    val reason: String,
    val isSelected: Boolean = false
)

enum class FolderFileCategory(val label: String) {
    ALL("All"),
    LARGE_FILE("Large Files"),
    DUPLICATE_FILE("Duplicates"),
    APK_OR_ARCHIVE("APKs & Archives"),
    GENERAL("Other Files")
}

data class ScannedFolderFile(
    val uriString: String,
    val documentId: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val lastModifiedMs: Long,
    val category: FolderFileCategory,
    val hint: String,
    val isSelected: Boolean = false
)

data class StorageScanSummary(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedPercentage: Int,
    val appCacheBytes: Long,
    val auditedMediaCount: Int,
    val auditedMediaReclaimBytes: Long,
    val scannedFolderName: String?,
    val scannedFilesCount: Int,
    val scannedFolderClutterBytes: Long
)

data class AiCleaningStep(
    val title: String,
    val description: String,
    val estimatedSavings: String,
    val priority: String
)

data class AiCleaningPlan(
    val headline: String,
    val summary: String,
    val steps: List<AiCleaningStep>,
    val isPlaceholder: Boolean = true
)

data class AdvisorMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isPlaceholder: Boolean = false
)

data class CleanovaUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val storageStats: DeviceStorageStats = DeviceStorageStats(),
    val lastFreedBytes: Long = 0L,
    val auditedMedia: List<AuditedMediaItem> = emptyList(),
    val selectedMediaFilter: MediaAuditCategory = MediaAuditCategory.ALL,
    val isAuditingMedia: Boolean = false,
    val scannedFolderName: String? = null,
    val scannedFiles: List<ScannedFolderFile> = emptyList(),
    val selectedFolderFilter: FolderFileCategory = FolderFileCategory.ALL,
    val isScanningFolder: Boolean = false,
    val aiPlan: AiCleaningPlan? = null,
    val advisorMessages: List<AdvisorMessage> = emptyList(),
    val isAdvisorLoading: Boolean = false,
    val bannerMessage: String? = null
) {
    val selectedMediaReclaimBytes: Long
        get() = auditedMedia.filter { it.isSelected }.sumOf { it.sizeBytes }

    val selectedFolderReclaimBytes: Long
        get() = scannedFiles.filter { it.isSelected }.sumOf { it.sizeBytes }

    val totalPotentialReclaimBytes: Long
        get() = storageStats.appCacheBytes +
            auditedMedia.filter { it.category != MediaAuditCategory.SCREENSHOT_OR_COMPACT || it.isSelected }
                .sumOf { if (it.isSelected) it.sizeBytes else 0L } +
            scannedFiles.filter { it.isSelected || it.category != FolderFileCategory.GENERAL }
                .sumOf { it.sizeBytes }
}
