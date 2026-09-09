package com.lumora.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationCoordinatorTest {

    @Test
    fun `default state preserves the legacy Live catalogue landing state`() {
        val coordinator = NavigationCoordinator()

        assertEquals(0, coordinator.state.activeTab)
        assertFalse(coordinator.state.showingHome)
        assertFalse(coordinator.state.showingDownloads)
        assertFalse(coordinator.state.showingDiscover)
        assertFalse(coordinator.state.showingCatchup)
        assertEquals(NavigationDestination.CATALOG, coordinator.state.destination)
    }

    @Test
    fun `atomic destinations clear incompatible legacy panes`() {
        val coordinator = NavigationCoordinator()

        coordinator.showHome()
        assertEquals(NavigationDestination.HOME, coordinator.state.destination)

        coordinator.showCatalogTab(2)
        assertEquals(2, coordinator.state.activeTab)
        assertEquals(NavigationDestination.CATALOG, coordinator.state.destination)
        assertFalse(coordinator.state.showingHome)

        coordinator.showDiscover()
        assertEquals(NavigationDestination.DISCOVER, coordinator.state.destination)

        coordinator.showDownloads()
        assertEquals(NavigationDestination.DOWNLOADS, coordinator.state.destination)

        coordinator.showCatchup()
        assertEquals(NavigationDestination.CATCHUP, coordinator.state.destination)
        assertFalse(coordinator.state.showingDownloads)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `catalog transition accepts only the three existing tabs`() {
        NavigationCoordinator().showCatalogTab(3)
    }

    @Test
    fun `compatibility mutators keep the legacy one-field-at-a-time behavior`() {
        val coordinator = NavigationCoordinator()

        coordinator.setActiveTab(2)
        coordinator.setShowingHome(true)

        assertEquals(2, coordinator.state.activeTab)
        assertTrue(coordinator.state.showingHome)
        assertEquals(NavigationDestination.HOME, coordinator.state.destination)
    }
}
