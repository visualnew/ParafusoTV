package com.lumora.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusStateTest {

    @Test
    fun `default state preserves the legacy hidden empty status`() {
        val state = StatusState()

        assertEquals("", state.statusText)
        assertFalse(state.statusWanted)
        assertFalse(state.isLoading)
    }

    @Test
    fun `update keeps status text and requested visibility together`() {
        val state = StatusState()

        state.update("Loading...", wanted = true)

        assertEquals("Loading...", state.statusText)
        assertTrue(state.statusWanted)
        assertTrue(state.isLoading)
    }

    @Test
    fun `empty hidden update preserves the existing status clear behavior`() {
        val state = StatusState()
        state.update("Loading...", wanted = true)

        state.update("", wanted = false)

        assertEquals("", state.statusText)
        assertFalse(state.statusWanted)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loading follows the legacy ASCII dots rule after trailing whitespace`() {
        val state = StatusState()

        state.update("Connecting...   ", wanted = true)
        assertTrue(state.isLoading)

        state.update("Connecting…", wanted = true)
        assertFalse(state.isLoading)

        state.update("3 items", wanted = true)
        assertFalse(state.isLoading)
    }
}
