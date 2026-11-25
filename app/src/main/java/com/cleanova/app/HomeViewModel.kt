package com.cleanova.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanova.app.domain.*
import com.cleanova.app.data.StorageRepository
import com.cleanova.app.core.permissions.MediaPermissions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val capacity: Capacity? = null,
    val inventory: MediaInventory? = null,
    val scanning: Boolean = false,
    val capacityError: Boolean = false,
    val scanError: String? = null
)
class HomeViewModel(app: Application): AndroidViewModel(app) {
    private val repository: StorageReader = StorageRepository(app)
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()
    private var scanJob: Job? = null
    private var refreshJob: Job? = null
    private var generation = 0L
    init { refresh() }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val value = repository.capacity()
                _state.update { it.copy(capacity = value, capacityError = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(capacityError = true) } }
        }
    }
    fun clearInventory() {
        cancelScan()
        _state.update { it.copy(inventory = null, scanError = null) }
    }
    fun scan() {
        if (scanJob?.isActive == true) return
        val access = MediaPermissions.read(getApplication<Application>())
        if (!access.hasAny) {
            clearInventory()
            return
        }
        val id = ++generation
        _state.update { it.copy(scanning = true, inventory = null, scanError = null) }
        scanJob = viewModelScope.launch {
            try {
                val result = repository.inventory(access)
                if (generation == id && MediaPermissions.read(getApplication<Application>()) == access) {
                    _state.update { it.copy(inventory = result) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (generation == id) _state.update { it.copy(scanError = "Could not read media. Check access and try again.") }
            } finally {
                if (generation == id) _state.update { it.copy(scanning = false) }
            }
        }
    }
    fun cancelScan() {
        generation++
        scanJob?.cancel()
        scanJob = null
        _state.update { it.copy(scanning = false) }
    }
}
