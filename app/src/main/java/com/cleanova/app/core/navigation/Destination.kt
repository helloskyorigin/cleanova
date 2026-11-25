package com.cleanova.app.core.navigation

enum class Destination { HOME, MEDIA, STORAGE, SETTINGS }

data class NavigationState(val tab: Destination = Destination.HOME, val settingsOpen: Boolean = false) {
    init { require(tab != Destination.SETTINGS) }
    val current: Destination get() = if (settingsOpen) Destination.SETTINGS else tab
    fun select(destination: Destination): NavigationState =
        if (destination == Destination.SETTINGS) copy(settingsOpen = true)
        else NavigationState(destination)
    fun back(): NavigationState = when {
        settingsOpen -> copy(settingsOpen = false)
        tab != Destination.HOME -> NavigationState()
        else -> this
    }
    val canGoBack: Boolean get() = settingsOpen || tab != Destination.HOME
}
