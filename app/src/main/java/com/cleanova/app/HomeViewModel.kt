package com.cleanova.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanova.app.core.permissions.MediaPermissions
import com.cleanova.app.core.storage.StorageSession
import com.cleanova.app.core.storage.StorageState
import com.cleanova.app.data.StorageRepository

// Retained name for the existing foundation UI. State and scan orchestration are platform-neutral.
typealias HomeState = StorageState

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val session = StorageSession(
        reader = StorageRepository(app),
        readAccess = { MediaPermissions.read(app) },
        scope = viewModelScope
    )
    val state = session.state
    init { refresh() }

    fun refresh() = session.refresh()
    fun scan() = session.scan()
    fun cancelScan() = session.cancelScan()
    fun clearInventory() = session.clearInventory()
    fun onResume() = session.onResume()
}
