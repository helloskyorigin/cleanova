package com.cleanova.app.core.storage

import com.cleanova.app.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanFailure { ACCESS_CHANGED, READ_FAILED }

sealed interface ScanStatus {
    data object Idle : ScanStatus
    data class Running(val access: MediaAccess) : ScanStatus
    data class Ready(val inventory: MediaInventory) : ScanStatus
    data class Failed(val reason: ScanFailure) : ScanStatus
}

data class StorageState(
    val capacity: Capacity? = null,
    val capacityError: Boolean = false,
    val scan: ScanStatus = ScanStatus.Idle
) {
    val inventory: MediaInventory? get() = (scan as? ScanStatus.Ready)?.inventory
    val scanning: Boolean get() = scan is ScanStatus.Running
    val scanError: String? get() = when ((scan as? ScanStatus.Failed)?.reason) {
        ScanFailure.ACCESS_CHANGED -> "Media access changed. Review permissions and scan again."
        ScanFailure.READ_FAILED -> "Could not read media. Check access and try again."
        null -> null
    }
}

/**
 * UI-independent scan coordinator. All commands and supplied scope must be confined to
 * the same serialized dispatcher (Main in the Android app). IO belongs in StorageReader.
 * Each scan/refresh has an identity so cancelled or obsolete work cannot publish results.
 */
class StorageSession(
    private val reader: StorageReader,
    private val readAccess: () -> MediaAccess,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(StorageState())
    val state = mutableState.asStateFlow()
    private var scanJob: Job? = null
    private var refreshJob: Job? = null
    private var scanGeneration = 0L
    private var refreshGeneration = 0L

    fun refresh() {
        val id = ++refreshGeneration
        refreshJob?.cancel()
        refreshJob = scope.launch {
            try {
                val value = reader.capacity()
                if (id == refreshGeneration) mutableState.update { it.copy(capacity = value, capacityError = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (id == refreshGeneration) mutableState.update { it.copy(capacityError = true) }
            }
        }
    }

    fun scan() {
        if (state.value.scanning) return
        val access = readAccess()
        if (!access.hasAny) {
            clearInventory()
            return
        }
        val id = ++scanGeneration
        mutableState.update { it.copy(scan = ScanStatus.Running(access)) }
        scanJob = scope.launch {
            try {
                val result = reader.inventory(access)
                if (id == scanGeneration) {
                    val next = if (readAccess() == access && result.access == access) ScanStatus.Ready(result)
                    else ScanStatus.Failed(ScanFailure.ACCESS_CHANGED)
                    mutableState.update { it.copy(scan = next) }
                }
            } catch (e: CancellationException) {
                if (id == scanGeneration) mutableState.update { it.copy(scan = ScanStatus.Idle) }
                throw e
            } catch (_: SecurityException) {
                if (id == scanGeneration) mutableState.update { it.copy(scan = ScanStatus.Failed(ScanFailure.ACCESS_CHANGED)) }
            } catch (_: Exception) {
                if (id == scanGeneration) mutableState.update { it.copy(scan = ScanStatus.Failed(ScanFailure.READ_FAILED)) }
            } finally {
                if (id == scanGeneration && state.value.scanning) {
                    mutableState.update { it.copy(scan = ScanStatus.Idle) }
                }
            }
        }
    }

    fun cancelScan() {
        ++scanGeneration
        scanJob?.cancel()
        scanJob = null
        if (state.value.scanning) mutableState.update { it.copy(scan = ScanStatus.Idle) }
    }

    fun clearInventory() {
        cancelScan()
        mutableState.update { it.copy(scan = ScanStatus.Idle) }
    }

    /** Call when app returns to foreground or media grants change. Selected sets may change
     * without a change in the grant enum; never retain such a snapshot across resume. */
    fun onResume() {
        val current = readAccess()
        val previous = when (val scan = state.value.scan) {
            is ScanStatus.Running -> scan.access
            is ScanStatus.Ready -> scan.inventory.access
            else -> null
        }
        if (previous != null && (previous != current || !current.isFull)) clearInventory()
        refresh()
    }
}
