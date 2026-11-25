package com.cleanova.app
import com.cleanova.app.core.navigation.*
import org.junit.Assert.*
import org.junit.Test
class NavigationTest {
    @Test fun startsHome() { assertEquals(Destination.HOME, NavigationState().current); assertFalse(NavigationState().canGoBack) }
    @Test fun settingsKeepsSelectedTab() {
        val state = NavigationState().select(Destination.MEDIA).select(Destination.SETTINGS)
        assertEquals(Destination.SETTINGS, state.current)
        assertEquals(Destination.MEDIA, state.back().current)
    }
    @Test fun backReturnsToHome() { assertEquals(Destination.HOME, NavigationState().select(Destination.STORAGE).back().current) }
    @Test fun selectingTabClosesSettings() { assertEquals(Destination.MEDIA, NavigationState().select(Destination.SETTINGS).select(Destination.MEDIA).current) }
}
