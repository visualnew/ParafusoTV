package com.lumora

import android.content.SharedPreferences
import com.lumora.data.IptvProviderMetaStore
import com.lumora.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderStatusIsolationTest {

    @Test
    fun `provider status and Xtream expiry stay isolated`() {
        val prefs = MemoryPreferences()

        IptvProviderMetaStore.saveSuccess(prefs, "one", 11, refreshedAt = 100L, xtreamExpirySeconds = 1_700_000_000L, xtreamAuthenticated = true)
        IptvProviderMetaStore.saveSuccess(prefs, "two", 22, refreshedAt = 200L, xtreamExpirySeconds = 1_800_000_000L, xtreamIsTrial = true, xtreamAuthenticated = true)

        val one = IptvProviderMetaStore.get(prefs, "one")
        val two = IptvProviderMetaStore.get(prefs, "two")
        assertEquals(11, one.itemCount)
        assertEquals(100L, one.refreshedAt)
        assertEquals(1_700_000_000L, one.xtreamExpirySeconds)
        assertFalse(one.xtreamIsTrial)
        assertEquals("authenticated", one.authenticationState)
        assertEquals(22, two.itemCount)
        assertEquals(200L, two.refreshedAt)
        assertEquals(1_800_000_000L, two.xtreamExpirySeconds)
        assertTrue(two.xtreamIsTrial)
        assertEquals("authenticated", two.authenticationState)
    }

    @Test
    fun `failure preserves last successful count and timestamp`() {
        val prefs = MemoryPreferences()
        IptvProviderMetaStore.saveSuccess(prefs, "m3u", 7, refreshedAt = 100L)
        IptvProviderMetaStore.saveFailure(prefs, "m3u", "connection reset")

        val meta = IptvProviderMetaStore.get(prefs, "m3u")
        assertEquals(7, meta.itemCount)
        assertEquals(100L, meta.refreshedAt)
        assertEquals("connection reset", meta.lastError)
        assertFalse(meta.authenticationFailed)
    }

    @Test
    fun `success clears only that providers error`() {
        val prefs = MemoryPreferences()
        IptvProviderMetaStore.saveFailure(prefs, "one", "bad credentials", authenticationFailed = true)
        IptvProviderMetaStore.saveFailure(prefs, "two", "offline")
        IptvProviderMetaStore.saveSuccess(prefs, "one", 3, refreshedAt = 300L)

        val one = IptvProviderMetaStore.get(prefs, "one")
        val two = IptvProviderMetaStore.get(prefs, "two")
        assertNull(one.lastError)
        assertFalse(one.authenticationFailed)
        assertEquals("offline", two.lastError)
    }

    @Test
    fun `legacy Xtream status migrates only when ownership is unambiguous`() {
        val single = MemoryPreferences().apply {
            edit().putString("xtream_exp_date", "1700000000").putBoolean("xtream_is_trial", true).apply()
        }
        IptvProviderMetaStore.migrateLegacyXtreamStatus(single, listOf("only"))
        assertEquals(1_700_000_000L, IptvProviderMetaStore.get(single, "only").xtreamExpirySeconds)
        assertTrue(IptvProviderMetaStore.get(single, "only").xtreamIsTrial)

        val multiple = MemoryPreferences().apply { edit().putString("xtream_exp_date", "1700000000").apply() }
        IptvProviderMetaStore.migrateLegacyXtreamStatus(multiple, listOf("first", "second"))
        assertNull(IptvProviderMetaStore.get(multiple, "first").xtreamExpirySeconds)
        assertNull(IptvProviderMetaStore.get(multiple, "second").xtreamExpirySeconds)
    }

    @Test
    fun `single provider replacement leaves other provider catalog untouched`() {
        val firstOld = channel("first-old", "first")
        val second = channel("second", "second")
        val firstNew = channel("first-new", "first")

        val result = replaceProviderCatalog(listOf(firstOld, second), "first", listOf(firstNew))

        assertEquals(listOf("second", "first-new"), result.map { it.id })
        assertEquals("second", result.first().sourceProviderId)
        assertEquals("first", result.last().sourceProviderId)
    }

    private fun channel(id: String, providerId: String) =
        Channel(id = id, name = id, url = "https://example.test/$id", sourceProviderId = providerId)
}

private class MemoryPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>)?.toMutableSet() ?: defValues
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private var clearRequested = false
        override fun putStringSet(key: String, values: MutableSet<String>?) = also { pending[key] = values?.toMutableSet() }
        override fun putString(key: String, value: String?) = also { pending[key] = value }
        override fun putInt(key: String, value: Int) = also { pending[key] = value }
        override fun putLong(key: String, value: Long) = also { pending[key] = value }
        override fun putFloat(key: String, value: Float) = also { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = also { pending[key] = value }
        override fun remove(key: String) = also { pending[key] = Removed }
        override fun clear() = also { clearRequested = true }
        override fun apply() {
            if (clearRequested) data.clear()
            pending.forEach { (key, value) -> if (value === Removed) data.remove(key) else data[key] = value }
        }
        override fun commit(): Boolean { apply(); return true }
    }
    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getInt(key: String, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = data.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    private companion object { val Removed = Any() }
}
