package com.example.data.ai

import com.example.data.model.AiCleaningPlan
import com.example.data.model.AiCleaningStep
import com.example.data.model.StorageScanSummary
import com.example.data.repository.StorageScannerRepository.Companion.formatBytes
import kotlinx.coroutines.delay

/**
 * Contract for Cleanova's AI declutter advisor.
 * Can be backed by Firebase AI Logic (Gemini) when Firebase is enabled,
 * or by PlaceholderAiCleanerAdvisor when Firebase setup is declined.
 */
interface AiCleanerAdvisor {
    suspend fun generateCleaningPlan(summary: StorageScanSummary): Result<AiCleaningPlan>
    suspend fun askAdvisor(question: String, summary: StorageScanSummary): Result<String>
}

class PlaceholderAiCleanerAdvisor : AiCleanerAdvisor {

    override suspend fun generateCleaningPlan(summary: StorageScanSummary): Result<AiCleaningPlan> {
        delay(450L)

        val freeLabel = formatBytes(summary.freeBytes)
        val cacheLabel = formatBytes(summary.appCacheBytes)
        val mediaReclaimLabel = formatBytes(summary.auditedMediaReclaimBytes)
        val folderClutterLabel = formatBytes(summary.scannedFolderClutterBytes)

        val steps = buildList {
            add(
                AiCleaningStep(
                    title = "Clear App Cache & Temporary Files",
                    description = "Sample recommendation: Sweep Cleanova's $cacheLabel local cache and open Android System Storage to trim cached data across heavy social or streaming apps.",
                    estimatedSavings = if (summary.appCacheBytes > 0L) cacheLabel else "Up to 1–3 GB system-wide",
                    priority = "Quick Win"
                )
            )
            add(
                AiCleaningStep(
                    title = if (summary.auditedMediaCount > 0) {
                        "Review ${summary.auditedMediaCount} Audited Media Items"
                    } else {
                        "Audit Recent Videos & Burst Photos"
                    },
                    description = if (summary.auditedMediaCount > 0) {
                        "Sample recommendation: You flagged $mediaReclaimLabel of duplicate or oversized media in the Media tab. Review and remove redundant clips first."
                    } else {
                        "Sample recommendation: Open the Media tab and pick recent camera rolls to spot 4K videos and duplicate shots without granting broad storage permissions."
                    },
                    estimatedSavings = if (summary.auditedMediaReclaimBytes > 0L) mediaReclaimLabel else "500 MB – 4 GB",
                    priority = "High Impact"
                )
            )
            add(
                AiCleaningStep(
                    title = if (summary.scannedFolderName != null) {
                        "Clean '${summary.scannedFolderName}' Folder"
                    } else {
                        "Deep-Scan Downloads Folder"
                    },
                    description = if (summary.scannedFolderName != null) {
                        "Sample recommendation: ${summary.scannedFilesCount} files inspected in '${summary.scannedFolderName}' with $folderClutterLabel of large/duplicate/archive candidates."
                    } else {
                        "Sample recommendation: Use Deep Scan to inspect your Downloads folder for forgotten APK installers, ZIP archives, and duplicate PDFs."
                    },
                    estimatedSavings = if (summary.scannedFolderClutterBytes > 0L) folderClutterLabel else "250 MB – 2 GB",
                    priority = "Safe Cleanup"
                )
            )
        }

        return Result.success(
            AiCleaningPlan(
                headline = "Sample Declutter Plan (${summary.usedPercentage}% Storage Used)",
                summary = "Sample AI overview: You currently have $freeLabel free. Prioritize oversized videos, duplicate downloads, and app caches for the fastest storage recovery.",
                steps = steps,
                isPlaceholder = true
            )
        )
    }

    override suspend fun askAdvisor(question: String, summary: StorageScanSummary): Result<String> {
        delay(380L)
        val q = question.lowercase()
        val freeLabel = formatBytes(summary.freeBytes)
        val response = when {
            q.contains("apk") || q.contains("download") || q.contains("zip") ->
                "[Sample AI Response] Old APK installers and extracted ZIP archives in Downloads can usually be deleted safely once the app is installed. Open the Deep Scan tab, select your Downloads folder, and filter by 'APKs & Archives' to remove them."
            q.contains("photo") || q.contains("video") || q.contains("duplicate") ->
                "[Sample AI Response] Videos typically consume 10x–40x more storage than photos. Use the Media tab's Photo Picker to audit recent batches—Cleanova automatically flags identical-size duplicate candidates and videos over 20 MB."
            q.contains("cache") || q.contains("fast") || q.contains("gb") ->
                "[Sample AI Response] With $freeLabel currently available, the fastest way to free 1–2 GB is clearing heavy app caches via System Storage Settings (linked on the Overview tab) and removing large videos or stale Downloads."
            else ->
                "[Sample AI Response] Based on your current device scan (${summary.usedPercentage}% used, $freeLabel free), start with a Deep Scan of your Downloads folder and audit your largest videos in the Media tab. Connect Firebase to unlock live Gemini-personalized advice."
        }
        return Result.success(response)
    }
}
