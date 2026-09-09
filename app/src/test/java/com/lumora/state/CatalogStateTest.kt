package com.lumora.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogStateTest {

    @Test
    fun `default state preserves the legacy empty catalogue`() {
        val state = CatalogState()

        assertTrue(state.allChannels.isEmpty())
        assertTrue(state.liveChannels.isEmpty())
        assertTrue(state.filmList.isEmpty())
        assertTrue(state.seriesList.isEmpty())
        assertTrue(state.filmShelves.isEmpty())
        assertTrue(state.seriesShelves.isEmpty())
        assertNull(state.selectedRowId)
        assertNull(state.selectedShelfItems)
        assertEquals(0, state.categoryFilterGeneration)
    }

    @Test
    fun `category selection and expansion retain their independent values`() {
        val state = CatalogState()

        state.selectedCategoryIds = setOf("sports")
        state.selectedBrandChannelIds = setOf("channel-1")
        state.selectedRowId = "row-1"
        state.selectedCategoryLabel = "Sports"
        state.categoryFilterGeneration += 1
        state.expandedGroupKeys += "bucket:sports"

        assertEquals(setOf("sports"), state.selectedCategoryIds)
        assertEquals(setOf("channel-1"), state.selectedBrandChannelIds)
        assertEquals("row-1", state.selectedRowId)
        assertEquals("Sports", state.selectedCategoryLabel)
        assertEquals(1, state.categoryFilterGeneration)
        assertTrue("bucket:sports" in state.expandedGroupKeys)
    }

    @Test
    fun `server home rows and up next memo keep mutable shared collections`() {
        val state = CatalogState()

        state.jellyfinResumeByServer["jellyfin-a"] = emptyList()
        state.plexNextUpByServer["plex-a"] = emptyList()
        state.upNextTiles["series-a"] = null
        state.upNextFetching += "series-b"
        state.upNextQueues["episode-a"] = emptyList()

        assertTrue("jellyfin-a" in state.jellyfinResumeByServer)
        assertTrue("plex-a" in state.plexNextUpByServer)
        assertTrue(state.upNextTiles.containsKey("series-a"))
        assertTrue("series-b" in state.upNextFetching)
        assertTrue(state.upNextQueues.containsKey("episode-a"))
        assertFalse(state.jellyfinResumeItems.isNotEmpty())
        assertFalse(state.plexNextUpItems.isNotEmpty())
    }
}
