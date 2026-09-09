package com.lumora.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePreferencesTest {

    @Test
    fun `profile 1 reads a legacy value until its scoped value exists`() {
        val storage = FakeStorage(strings = mutableMapOf("list" to "legacy"))
        val preferences = ProfilePreferences(storage) { 1 }

        assertEquals("legacy", preferences.getString("list"))
    }

    @Test
    fun `profile 1 prefers its scoped value over legacy`() {
        val storage = FakeStorage(strings = mutableMapOf(
            "list" to "legacy",
            "list_profile_1" to "profile one"
        ))
        val preferences = ProfilePreferences(storage) { 1 }

        assertEquals("profile one", preferences.getString("list"))
    }

    @Test
    fun `other profiles never inherit legacy values`() {
        val storage = FakeStorage(stringSets = mutableMapOf("hidden_home_shelves" to setOf("Legacy")))
        val preferences = ProfilePreferences(storage) { 2 }

        assertEquals(emptySet<String>(), preferences.getStringSet("hidden_home_shelves"))
    }

    @Test
    fun `boolean values retain their defaults and follow the same profile fallback`() {
        val storage = FakeStorage(booleans = mutableMapOf("group_channels" to false))
        var activeProfileId = 1
        val preferences = ProfilePreferences(storage) { activeProfileId }

        assertFalse(preferences.getBoolean("group_channels", true))
        preferences.putBoolean("group_channels", true)
        assertTrue(preferences.getBoolean("group_channels", false))
        activeProfileId = 2
        assertTrue(preferences.getBoolean("group_channels", true))
        preferences.putBoolean("group_channels", false)

        assertFalse(storage.booleans["group_channels_profile_2"] ?: true)
        assertFalse(preferences.getBoolean("group_channels", true))
    }

    @Test
    fun `first write creates the active profile namespace`() {
        val storage = FakeStorage(strings = mutableMapOf("list" to "legacy"))
        val preferences = ProfilePreferences(storage) { 2 }

        preferences.putString("list", "profile two")

        assertEquals("profile two", storage.strings["list_profile_2"])
        assertEquals("legacy", storage.strings["list"])
    }

    @Test
    fun `clearing a collection keeps an explicit empty scoped value and does not resurrect legacy`() {
        val storage = FakeStorage(stringSets = mutableMapOf("up_next_hidden_series" to setOf("legacy-series")))
        val preferences = ProfilePreferences(storage) { 1 }

        preferences.clearStringSet("up_next_hidden_series")

        assertTrue(storage.stringSets.containsKey("up_next_hidden_series_profile_1"))
        assertEquals(emptySet<String>(), preferences.getStringSet("up_next_hidden_series"))
    }

    @Test
    fun `writes remain isolated between profiles`() {
        val storage = FakeStorage()
        var activeProfileId = 1
        val preferences = ProfilePreferences(storage) { activeProfileId }

        preferences.putStringSet("pinned_categories_live", setOf("news"))
        activeProfileId = 2
        preferences.putStringSet("pinned_categories_live", setOf("sports"))

        assertEquals(setOf("sports"), preferences.getStringSet("pinned_categories_live"))
        activeProfileId = 1
        assertEquals(setOf("news"), preferences.getStringSet("pinned_categories_live"))
        assertFalse(storage.stringSets["pinned_categories_live_profile_1"] == storage.stringSets["pinned_categories_live_profile_2"])
    }

    private class FakeStorage(
        val strings: MutableMap<String, String> = mutableMapOf(),
        val booleans: MutableMap<String, Boolean> = mutableMapOf(),
        val stringSets: MutableMap<String, Set<String>> = mutableMapOf()
    ) : ProfilePreferences.Storage {
        override fun contains(key: String): Boolean = key in strings || key in booleans || key in stringSets

        override fun getString(key: String): String? = strings[key]

        override fun getBoolean(key: String): Boolean? = booleans[key]

        override fun getStringSet(key: String): Set<String>? = stringSets[key]?.toSet()

        override fun putString(key: String, value: String) {
            strings[key] = value
        }

        override fun putBoolean(key: String, value: Boolean) {
            booleans[key] = value
        }

        override fun putStringSet(key: String, value: Set<String>) {
            stringSets[key] = value.toSet()
        }
    }
}
