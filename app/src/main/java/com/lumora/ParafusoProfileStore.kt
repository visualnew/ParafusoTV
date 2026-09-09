package com.lumora

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ParafusoProfile(
    val id: Int,
    val name: String,
    val child: Boolean = false,
    val accent: String = "silver"
)

/** Local-only profile store. Nothing leaves the device. */
class ParafusoProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("parafuso_profiles", Context.MODE_PRIVATE)

    fun profiles(): List<ParafusoProfile> {
        val raw = prefs.getString("profiles", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                ParafusoProfile(o.getInt("id"), o.getString("name"), o.optBoolean("child"), o.optString("accent", "silver"))
            }
        }.getOrDefault(emptyList())
    }

    fun save(list: List<ParafusoProfile>) {
        val a = JSONArray()
        list.take(4).forEach { p ->
            a.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("child", p.child); put("accent", p.accent)
            })
        }
        prefs.edit().putString("profiles", a.toString()).apply()
    }

    fun activeId(): Int = prefs.getInt("active_id", -1)
    fun setActive(id: Int) {
        prefs.edit().putInt("active_id", id).apply()
        // The profile namespace is part of every local viewing store. Drop in-memory mirrors
        // immediately so switching profiles in the same Activity cannot leak the previous
        // profile's favourites/history into the newly selected one.
        com.lumora.cache.FavoritesStore.clearMemory()
        com.lumora.cache.RecentlyPlayedStore.clearMemory()
        com.lumora.cache.WatchedStore.clearMemory()
        com.lumora.cache.PlaybackPositionStore.clearMemory()
    }

    fun ensureDefault(): List<ParafusoProfile> {
        val existing = profiles()
        if (existing.isNotEmpty()) return existing
        val created = listOf(ParafusoProfile(1, "Principal"))
        save(created)
        return created
    }
}
