package com.lumora.data

import android.content.SharedPreferences
import org.json.JSONObject

/** Small local metadata store for the ParafusoTV provider manager.
 *  Catalog content remains in ChannelCache; this only remembers the last successful
 *  catalog refresh and the last known per-provider item counts shown in Settings. */
object IptvProviderMetaStore {
    private const val KEY = "parafusotv_provider_meta"

    data class Meta(
        val refreshedAt: Long = 0L,
        val itemCount: Int = 0
    )

    fun get(prefs: SharedPreferences, providerId: String): Meta {
        val root = runCatching { JSONObject(prefs.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())
        val o = root.optJSONObject(providerId) ?: return Meta()
        return Meta(o.optLong("refreshedAt", 0L), o.optInt("itemCount", 0))
    }

    fun save(prefs: SharedPreferences, providerId: String, itemCount: Int, refreshedAt: Long = System.currentTimeMillis()) {
        val root = runCatching { JSONObject(prefs.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())
        root.put(providerId, JSONObject().apply {
            put("refreshedAt", refreshedAt)
            put("itemCount", itemCount.coerceAtLeast(0))
        })
        prefs.edit().putString(KEY, root.toString()).apply()
    }
}
