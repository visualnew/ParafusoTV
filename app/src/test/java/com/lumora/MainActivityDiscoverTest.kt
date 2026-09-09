package com.lumora

import com.lumora.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MainActivityDiscoverTest {

    @Test
    fun `TV Home catalog lookup preserves the first occurrence of duplicate ids`() {
        val first = Channel(id = "shared", name = "First", url = "first")
        val second = Channel(id = "shared", name = "Second", url = "second")

        val lookup = firstCatalogEntriesById(listOf(first, second, Channel(id = "", name = "Blank", url = "blank")))

        assertEquals(first, lookup["shared"])
        assertFalse(lookup.containsKey(""))
    }
}
