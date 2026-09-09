package com.lumora.cache

import android.content.Context
import com.lumora.ParafusoProfileScope

private const val PREFS_NAME = "iptv_prefs"
private const val KEY_FAVORITE_SERIES = "favorite_series_ids"
private const val KEY_FAVORITE_CHANNELS = "favorite_channel_ids"

/** Favorited series (shown on Home) and favorited live channels (Favourites category in Live TV). */
object FavoritesStore {
    private val lock = Any()

    /**
     * In-memory mirror of the two prefs keys, filled lazily on first read of each and kept in
     * step by every writer below. Nothing outside this object writes those keys, so the map
     * cannot go stale behind it.
     *
     * Without this, [readSet] ran `getStringSet(...).toSet()` - a full copy of the user's
     * favourites - on every call, and the live guide calls [getFavoriteChannelIds] once per
     * row bind. Scrolling a large guide therefore allocated one copy of the whole favourites
     * set per row, per pass. Same reasoning as [ReminderStore], which the guide's other
     * per-row lookup already goes through.
     *
     * Values are immutable snapshots, so a caller holding one can't mutate what the next
     * reader sees; writers replace the entry rather than editing it in place.
     */
    private val cache = mutableMapOf<String, Set<String>>()

    fun clearMemory() = synchronized(lock) { cache.clear() }

    fun isFavoriteSeries(context: Context, id: String): Boolean = id in getFavoriteSeriesIds(context)

    fun toggleFavoriteSeries(context: Context, id: String): Boolean = toggle(context, ParafusoProfileScope.key(KEY_FAVORITE_SERIES, context), id)

    fun getFavoriteSeriesIds(context: Context): Set<String> = readSet(context, ParafusoProfileScope.key(KEY_FAVORITE_SERIES, context))

    /** Sets membership outright rather than flipping it - for reconciling against a server
     *  that owns the truth (Jellyfin's UserData.IsFavorite), where a toggle can't express
     *  "the server says this is no longer a favourite". Returns true if anything changed. */
    fun setFavoriteSeries(context: Context, id: String, favorite: Boolean): Boolean = synchronized(lock) {
        if (id.isBlank()) return false
        val current = readSet(context, ParafusoProfileScope.key(KEY_FAVORITE_SERIES, context))
        if (favorite == (id in current)) return false
        val updated = if (favorite) current + id else current - id
        write(context, KEY_FAVORITE_SERIES, updated)
        true
    }

    fun toggleFavoriteChannel(context: Context, id: String): Boolean = toggle(context, ParafusoProfileScope.key(KEY_FAVORITE_CHANNELS, context), id)

    fun getFavoriteChannelIds(context: Context): Set<String> = readSet(context, ParafusoProfileScope.key(KEY_FAVORITE_CHANNELS, context))

    /** Replaces both favourites sets outright - for backup restore, where the backup is the
     *  truth and anything currently stored must go. Updates the in-memory mirror like every
     *  other writer, so the cache cannot go stale. */
    fun replaceAll(context: Context, seriesIds: Set<String>, channelIds: Set<String>) {
        synchronized(lock) {
            write(context, ParafusoProfileScope.key(KEY_FAVORITE_SERIES, context), seriesIds)
            write(context, ParafusoProfileScope.key(KEY_FAVORITE_CHANNELS, context), channelIds)
        }
    }

    /** Returns the new membership state (true = now favorited). */
    private fun toggle(context: Context, key: String, id: String): Boolean = synchronized(lock) {
        val current = readSet(context, key)
        val nowFavorite = id !in current
        write(context, key, if (nowFavorite) current + id else current - id)
        nowFavorite
    }

    private fun write(context: Context, key: String, value: Set<String>) {
        cache[key] = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(key, value).apply()
    }

    private fun readSet(context: Context, key: String): Set<String> = synchronized(lock) {
        cache.getOrPut(key) {
            // Copied out of the pref rather than handed over: SharedPreferences returns its own
            // live instance from getStringSet, and the docs are explicit that mutating it (or
            // reading it after a later write) is undefined.
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val scoped = prefs.getStringSet(key, null)
            if (scoped != null) scoped.toSet()
            else if (com.lumora.ParafusoProfileScope.activeId(context) == 1) {
                // Preserve an existing pre-profile installation when the first profile is used.
                prefs.getStringSet(key.substringBefore("_profile_"), emptySet())?.toSet() ?: emptySet()
            } else emptySet()
        }
    }
}
