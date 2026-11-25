package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiCleanerAdvisor
import com.example.data.ai.PlaceholderAiCleanerAdvisor
import com.example.data.model.AdvisorMessage
import com.example.data.model.CleanovaUiState
import com.example.data.model.FolderFileCategory
import com.example.data.model.MediaAuditCategory
import com.example.data.model.StorageScanSummary
import com.example.data.repository.StorageScannerRepository
import com.example.data.repository.StorageScannerRepository.Companion.formatBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CleanovaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StorageScannerRepository(application.applicationContext)
    private val aiAdvisor: AiCleanerAdvisor = PlaceholderAiCleanerAdvisor()

    private val _uiState = MutableStateFlow(CleanovaUiState())
    val uiState: StateFlow<CleanovaUiState> = _uiState.asStateFlow()

    init {
        refreshStorageAndPlan()
    }

    fun refreshStorageAndPlan() {
        viewModelScope.launch {
            val stats = repository.readDeviceStorageStats()
            _uiState.update { it.copy(storageStats = stats) }
            generateAiPlan()
        }
    }

    fun runSmartScan() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanProgress = 0.1f,
                    bannerMessage = null
                )
            }
            for (step in 2..9) {
                delay(90L)
                _uiState.update { it.copy(scanProgress = step / 10f) }
            }
            val stats = repository.readDeviceStorageStats()
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanProgress = 1f,
                    storageStats = stats,
                    bannerMessage = "Scan complete • ${formatBytes(stats.freeBytes)} available on device"
                )
            }
            generateAiPlan()
        }
    }

    fun sweepAppCache() {
        viewModelScope.launch {
            val freed = repository.clearAppCache()
            val updatedStats = repository.readDeviceStorageStats()
            _uiState.update {
                it.copy(
                    storageStats = updatedStats,
                    lastFreedBytes = it.lastFreedBytes + freed,
                    bannerMessage = if (freed > 0L) {
                        "Swept ${formatBytes(freed)} of app cache"
                    } else {
                        "Cleanova cache is already spotless (0 B)"
                    }
                )
            }
            generateAiPlan()
        }
    }

    fun auditPickedMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuditingMedia = true) }
            val items = repository.inspectPickedMedia(uris)
            _uiState.update {
                it.copy(
                    auditedMedia = items,
                    isAuditingMedia = false,
                    selectedMediaFilter = MediaAuditCategory.ALL,
                    bannerMessage = "Audited ${items.size} media items"
                )
            }
            generateAiPlan()
        }
    }

    fun setMediaFilter(category: MediaAuditCategory) {
        _uiState.update { it.copy(selectedMediaFilter = category) }
    }

    fun toggleMediaSelection(uriString: String) {
        _uiState.update { state ->
            state.copy(
                auditedMedia = state.auditedMedia.map { item ->
                    if (item.uriString == uriString) item.copy(isSelected = !item.isSelected) else item
                }
            )
        }
    }

    fun removeSelectedMediaFromAuditList() {
        _uiState.update { state ->
            val removedBytes = state.selectedMediaReclaimBytes
            val remaining = state.auditedMedia.filterNot { it.isSelected }
            state.copy(
                auditedMedia = remaining,
                bannerMessage = if (removedBytes > 0L) {
                    "Cleared ${formatBytes(removedBytes)} from audit queue (photos remain safe in device library)"
                } else {
                    "No items selected"
                }
            )
        }
        generateAiPlan()
    }

    fun scanFolder(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningFolder = true) }
            val (folderName, files) = repository.scanFolderTree(treeUri)
            _uiState.update {
                it.copy(
                    scannedFolderName = folderName,
                    scannedFiles = files,
                    selectedFolderFilter = FolderFileCategory.ALL,
                    isScanningFolder = false,
                    bannerMessage = "Scanned ${files.size} files in $folderName"
                )
            }
            generateAiPlan()
        }
    }

    fun setFolderFilter(category: FolderFileCategory) {
        _uiState.update { it.copy(selectedFolderFilter = category) }
    }

    fun toggleFolderFileSelection(uriString: String) {
        _uiState.update { state ->
            state.copy(
                scannedFiles = state.scannedFiles.map { file ->
                    if (file.uriString == uriString) file.copy(isSelected = !file.isSelected) else file
                }
            )
        }
    }

    fun selectAllVisibleFolderFiles(select: Boolean) {
        _uiState.update { state ->
            val filter = state.selectedFolderFilter
            state.copy(
                scannedFiles = state.scannedFiles.map { file ->
                    if (filter == FolderFileCategory.ALL || file.category == filter) {
                        file.copy(isSelected = select)
                    } else {
                        file
                    }
                }
            )
        }
    }

    fun deleteSelectedFolderFiles() {
        val selected = _uiState.value.scannedFiles.filter { it.isSelected }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val (deletedCount, freedBytes) = repository.deleteFolderFiles(selected)
            val deletedUris = selected.map { it.uriString }.toSet()
            val updatedStats = repository.readDeviceStorageStats()
            _uiState.update { state ->
                state.copy(
                    storageStats = updatedStats,
                    scannedFiles = state.scannedFiles.filterNot { it.uriString in deletedUris && deletedCount > 0 },
                    lastFreedBytes = state.lastFreedBytes + freedBytes,
                    bannerMessage = if (deletedCount > 0) {
                        "Deleted $deletedCount file(s) • Freed ${formatBytes(freedBytes)}"
                    } else {
                        "Could not delete selected files in this folder"
                    }
                )
            }
            generateAiPlan()
        }
    }

    fun generateAiPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdvisorLoading = true) }
            val summary = buildScanSummary()
            val result = aiAdvisor.generateCleaningPlan(summary)
            result.onSuccess { plan ->
                _uiState.update {
                    it.copy(
                        aiPlan = plan,
                        isAdvisorLoading = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isAdvisorLoading = false) }
            }
        }
    }

    fun askAdvisor(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val userMsg = AdvisorMessage(
                id = UUID.randomUUID().toString(),
                isUser = true,
                text = trimmed
            )
            _uiState.update {
                it.copy(
                    advisorMessages = it.advisorMessages + userMsg,
                    isAdvisorLoading = true
                )
            }
            val result = aiAdvisor.askAdvisor(trimmed, buildScanSummary())
            result.onSuccess { answer ->
                val aiMsg = AdvisorMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = answer,
                    isPlaceholder = true
                )
                _uiState.update {
                    it.copy(
                        advisorMessages = it.advisorMessages + aiMsg,
                        isAdvisorLoading = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isAdvisorLoading = false) }
            }
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    private fun buildScanSummary(): StorageScanSummary {
        val state = _uiState.value
        val stats = state.storageStats
        val usedPct = (stats.usedFraction * 100f).toInt()
        val clutterBytes = state.scannedFiles
            .filter { it.category != FolderFileCategory.GENERAL }
            .sumOf { it.sizeBytes }

        return StorageScanSummary(
            totalBytes = stats.totalBytes,
            freeBytes = stats.freeBytes,
            usedPercentage = usedPct,
            appCacheBytes = stats.appCacheBytes,
            auditedMediaCount = state.auditedMedia.size,
            auditedMediaReclaimBytes = state.auditedMedia.filter { it.isSelected }.sumOf { it.sizeBytes },
            scannedFolderName = state.scannedFolderName,
            scannedFilesCount = state.scannedFiles.size,
            scannedFolderClutterBytes = clutterBytes
        )
    }
}
