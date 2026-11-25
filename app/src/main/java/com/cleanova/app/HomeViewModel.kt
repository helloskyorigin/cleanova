package com.cleanova.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanova.app.data.Capacity
import com.cleanova.app.data.MediaInventory
import com.cleanova.app.data.StorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class HomeState(
    val capacity: Capacity? = null,
    val inventory: MediaInventory? = null,
    val scanning: Boolean = false,
    val capacityError: Boolean = false,
    val scanError: String? = null
)
class HomeViewModel(app: Application): AndroidViewModel(app) {
    private val repository = StorageRepository(app)
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()
    private var scanJob: Job? = null
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        try {
            val value = repository.capacity()
            _state.update { it.copy(capacity = value, capacityError = false) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { _state.update { it.copy(capacityError = true) } }
    }
    fun clearInventory() {
        cancelScan()
        _state.update { it.copy(inventory = null, scanError = null) }
    }
    fun scan() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _state.update { it.copy(scanning = true, inventory = null, scanError = null) }
            try {
                val result = repository.inventory()
                _state.update { it.copy(inventory = result) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(scanError = "Could not read media. Check access and try again.") } }
            finally { _state.update { it.copy(scanning = false) } }
        }
    }
    fun cancelScan() { scanJob?.cancel() }
}
