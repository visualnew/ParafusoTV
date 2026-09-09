package com.lumora.state

import android.content.Context
import android.content.SharedPreferences
import com.lumora.ParafusoProfileScope

/**
 * Profile-scoped access to a small set of legacy SharedPreferences values.
 *
 * Values written through this boundary always use a profile namespace. Existing unscoped
 * values remain readable only by the legacy/default profile until that profile writes its own
 * value, so old installations retain their data without copying it to every profile.
 */
internal class ProfilePreferences internal constructor(
    private val storage: Storage,
    private val activeProfileId: () -> Int
) {
    internal constructor(sharedPreferences: SharedPreferences, context: Context) : this(
        SharedPreferencesStorage(sharedPreferences),
        { ParafusoProfileScope.activeId(context) }
    )

    internal fun getString(key: String, defaultValue: String = ""): String {
        val scopedKey = scopedKey(key)
        return when {
            storage.contains(scopedKey) -> storage.getString(scopedKey) ?: defaultValue
            profileId() == LEGACY_PROFILE_ID -> storage.getString(key) ?: defaultValue
            else -> defaultValue
        }
    }

    internal fun putString(key: String, value: String) {
        storage.putString(scopedKey(key), value)
    }

    internal fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        val scopedKey = scopedKey(key)
        val value = when {
            storage.contains(scopedKey) -> storage.getStringSet(scopedKey)
            profileId() == LEGACY_PROFILE_ID -> storage.getStringSet(key)
            else -> null
        }
        return value?.toSet() ?: defaultValue.toSet()
    }

    internal fun putStringSet(key: String, value: Set<String>) {
        storage.putStringSet(scopedKey(key), value)
    }

    /**
     * Clearing writes an explicit empty profile value instead of removing it. Otherwise profile
     * 1 would fall back to and resurrect the legacy global collection.
     */
    internal fun clearStringSet(key: String) {
        putStringSet(key, emptySet())
    }

    internal fun scopedKey(key: String): String = "${key}_profile_${profileId()}"

    private fun profileId(): Int = activeProfileId().takeIf { it > 0 } ?: LEGACY_PROFILE_ID

    internal interface Storage {
        fun contains(key: String): Boolean
        fun getString(key: String): String?
        fun getStringSet(key: String): Set<String>?
        fun putString(key: String, value: String)
        fun putStringSet(key: String, value: Set<String>)
    }

    private class SharedPreferencesStorage(
        private val sharedPreferences: SharedPreferences
    ) : Storage {
        override fun contains(key: String): Boolean = sharedPreferences.contains(key)

        override fun getString(key: String): String? = sharedPreferences.getString(key, null)

        override fun getStringSet(key: String): Set<String>? = sharedPreferences.getStringSet(key, null)

        override fun putString(key: String, value: String) {
            sharedPreferences.edit().putString(key, value).apply()
        }

        override fun putStringSet(key: String, value: Set<String>) {
            sharedPreferences.edit().putStringSet(key, value).apply()
        }
    }

    private companion object {
        const val LEGACY_PROFILE_ID = 1
    }
}
