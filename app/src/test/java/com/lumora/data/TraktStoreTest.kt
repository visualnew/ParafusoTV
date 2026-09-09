package com.lumora.data

import android.content.SharedPreferences
import com.lumora.data.remote.trakt.TraktClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktStoreTest {

    @Test
    fun `profile 1 reads the legacy session until it has an override`() {
        val prefs = FakeSharedPreferences().apply { legacyTokens("legacy-access", "legacy-refresh", 101L) }

        assertEquals("legacy-access", TraktStore.tokens(prefs, 1)?.accessToken)

        TraktStore.saveTokens(prefs, 1, tokens("profile-one-access", "profile-one-refresh", 202L))

        assertEquals("profile-one-access", TraktStore.tokens(prefs, 1)?.accessToken)
        assertEquals("legacy-access", prefs.getString("trakt_access_token", null))
    }

    @Test
    fun `other profiles never inherit the legacy session`() {
        val prefs = FakeSharedPreferences().apply { legacyTokens("legacy-access", "legacy-refresh", 101L) }

        assertNull(TraktStore.tokens(prefs, 2))
        assertFalse(TraktStore.isSignedIn(prefs, 2))
    }

    @Test
    fun `login and refresh writes stay with their owner profile`() {
        val prefs = FakeSharedPreferences()
        TraktStore.saveTokens(prefs, 1, tokens("one-access", "one-refresh", 1L))
        TraktStore.saveTokens(prefs, 2, tokens("two-access", "two-refresh", 2L))
        TraktStore.saveUsername(prefs, 1, "one-user")
        TraktStore.saveUsername(prefs, 2, "two-user")
        // saveTokens is also the persistence path after a successful refresh.
        TraktStore.saveTokens(prefs, 2, tokens("two-refreshed", "two-refresh-next", 3L))

        assertEquals("one-access", TraktStore.tokens(prefs, 1)?.accessToken)
        assertEquals("two-refreshed", TraktStore.tokens(prefs, 2)?.accessToken)
        assertEquals(3L, TraktStore.tokens(prefs, 2)?.expiresAtMs)
        assertEquals("one-user", TraktStore.username(prefs, 1))
        assertEquals("two-user", TraktStore.username(prefs, 2))
    }

    @Test
    fun `logout affects only its profile and shadows the profile 1 legacy session`() {
        val prefs = FakeSharedPreferences().apply { legacyTokens("legacy-access", "legacy-refresh", 1L) }
        TraktStore.saveTokens(prefs, 2, tokens("two-access", "two-refresh", 2L))

        TraktStore.clear(prefs, 1)

        assertNull(TraktStore.tokens(prefs, 1))
        assertTrue(prefs.contains("trakt_access_token_profile_1"))
        assertEquals("two-access", TraktStore.tokens(prefs, 2)?.accessToken)
        assertEquals("legacy-access", prefs.getString("trakt_access_token", null))

        TraktStore.clear(prefs, 2)
        assertNull(TraktStore.tokens(prefs, 2))
        assertNull(TraktStore.tokens(prefs, 1))
    }

    @Test
    fun `toggles pull timestamp and id memo are profile isolated`() {
        val prefs = FakeSharedPreferences().apply {
            edit()
                .putBoolean("trakt_scrobble_enabled", false)
                .putBoolean("trakt_watched_sync_enabled", true)
                .putLong("trakt_last_watched_pull", 44L)
                .putStringSet("trakt_tmdb_id_memo", mutableSetOf("m|legacy|7"))
                .apply()
        }

        assertFalse(TraktStore.isScrobbleEnabled(prefs, 1))
        assertTrue(TraktStore.isWatchedSyncEnabled(prefs, 1))
        assertEquals(44L, TraktStore.lastWatchedPullMs(prefs, 1))
        assertEquals(mapOf("m|legacy" to 7), TraktStore.idMemo(prefs, 1))

        assertTrue(TraktStore.isScrobbleEnabled(prefs, 2))
        assertFalse(TraktStore.isWatchedSyncEnabled(prefs, 2))
        assertEquals(0L, TraktStore.lastWatchedPullMs(prefs, 2))
        assertEquals(emptyMap<String, Int>(), TraktStore.idMemo(prefs, 2))

        TraktStore.setScrobbleEnabled(prefs, 2, false)
        TraktStore.setWatchedSyncEnabled(prefs, 2, true)
        TraktStore.saveIdMemo(prefs, 2, mapOf("m|two" to 8))
        TraktStore.markWatchedPulled(prefs, 2)

        assertFalse(TraktStore.isScrobbleEnabled(prefs, 2))
        assertTrue(TraktStore.isWatchedSyncEnabled(prefs, 2))
        assertEquals(mapOf("m|two" to 8), TraktStore.idMemo(prefs, 2))
        assertTrue(TraktStore.lastWatchedPullMs(prefs, 2) > 0L)
        assertEquals(44L, TraktStore.lastWatchedPullMs(prefs, 1))
    }

    @Test
    fun `an asynchronous result is discarded after its owner profile changes`() {
        val ownerProfileId = 1
        val activeProfileId = 2
        var applied = false

        if (TraktStore.isCurrentProfile(ownerProfileId, activeProfileId)) applied = true

        assertFalse(applied)
        assertFalse(TraktStore.isCurrentProfile(ownerProfileId, activeProfileId))
    }

    private fun tokens(access: String, refresh: String, expiresAt: Long) =
        TraktClient.Tokens(access, refresh, expiresAt)

    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        fun legacyTokens(access: String, refresh: String, expiresAt: Long) {
            edit()
                .putString("trakt_access_token", access)
                .putString("trakt_refresh_token", refresh)
                .putLong("trakt_expires_at", expiresAt)
                .apply()
        }

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>)?.toMutableSet() ?: defValues

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearRequested = false

            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor =
                also { pending[key] = values?.toMutableSet() }

            override fun putString(key: String, value: String?): SharedPreferences.Editor =
                also { pending[key] = value }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor = also { pending[key] = value }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = also { pending[key] = value }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = also { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = also { pending[key] = value }
            override fun remove(key: String): SharedPreferences.Editor = also { pending[key] = Removed }
            override fun clear(): SharedPreferences.Editor = also { clearRequested = true }

            override fun apply() {
                if (clearRequested) data.clear()
                pending.forEach { (key, value) -> if (value === Removed) data.remove(key) else data[key] = value }
            }

            override fun commit(): Boolean {
                apply()
                return true
            }
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

        private companion object {
            val Removed = Any()
        }
    }
}
