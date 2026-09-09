package com.lumora.data

import android.content.SharedPreferences
import com.lumora.data.remote.trakt.TraktClient

/**
 * The stored Trakt session and its two feature toggles.
 *
 * Deliberately not part of [IptvProviderStore] or [MediaServerStore]: Trakt is neither a source
 * of content nor a library to browse. Nothing here ever produces a [com.lumora.model.Channel] -
 * it is one account whose only job is to be told what was watched. Its values still live in the
 * same "iptv_prefs" file everything else uses, but each is scoped to the active ParafusoTV
 * profile. Legacy flat keys remain readable only for profile 1 until that profile writes its own
 * value; they are never copied or removed automatically.
 *
 * The two toggles are kept apart on purpose, because they answer different questions and people
 * want them set differently: [isScrobbleEnabled] governs live reporting from the player (which
 * shows up on your Trakt profile as you watch), while [isWatchedSyncEnabled] governs history
 * writes and the startup pull (which quietly reconciles state). Someone happy for Lumora to
 * mirror their watched marks is not necessarily happy for every play to broadcast.
 */
object TraktStore {

    private const val KEY_ACCESS_TOKEN = "trakt_access_token"
    private const val KEY_REFRESH_TOKEN = "trakt_refresh_token"
    private const val KEY_EXPIRES_AT = "trakt_expires_at"
    private const val KEY_USERNAME = "trakt_username"
    private const val KEY_SCROBBLE = "trakt_scrobble_enabled"
    private const val KEY_WATCHED_SYNC = "trakt_watched_sync_enabled"
    /** Timestamp of the last successful `/sync/watched` pull, so a cold start that already
     *  reconciled today doesn't do it again on every launch. */
    private const val KEY_LAST_PULL = "trakt_last_watched_pull"

    fun tokens(prefs: SharedPreferences, profileId: Int): TraktClient.Tokens? {
        val access = getString(prefs, KEY_ACCESS_TOKEN, profileId)?.takeIf { it.isNotBlank() } ?: return null
        val refresh = getString(prefs, KEY_REFRESH_TOKEN, profileId)?.takeIf { it.isNotBlank() } ?: return null
        return TraktClient.Tokens(access, refresh, getLong(prefs, KEY_EXPIRES_AT, profileId, 0L))
    }

    fun saveTokens(prefs: SharedPreferences, profileId: Int, tokens: TraktClient.Tokens) {
        prefs.edit()
            .putString(scopedKey(KEY_ACCESS_TOKEN, profileId), tokens.accessToken)
            .putString(scopedKey(KEY_REFRESH_TOKEN, profileId), tokens.refreshToken)
            .putLong(scopedKey(KEY_EXPIRES_AT, profileId), tokens.expiresAtMs)
            .apply()
    }

    fun username(prefs: SharedPreferences, profileId: Int): String? =
        getString(prefs, KEY_USERNAME, profileId)?.takeIf { it.isNotBlank() }

    fun saveUsername(prefs: SharedPreferences, profileId: Int, username: String?) {
        prefs.edit().putString(scopedKey(KEY_USERNAME, profileId), username.orEmpty()).apply()
    }

    fun isSignedIn(prefs: SharedPreferences, profileId: Int): Boolean = tokens(prefs, profileId) != null

    /**
     * Drops the whole session.
     *
     * The toggles are deliberately left alone: they are a preference about how Trakt should
     * behave, not part of the session, and someone who signs out and back in a minute later
     * shouldn't have to set them again. They are inert while signed out anyway - every caller
     * checks for a token first.
     */
    fun clear(prefs: SharedPreferences, profileId: Int) {
        prefs.edit()
            // Empty scoped values deliberately shadow a profile-1 legacy session after logout.
            .putString(scopedKey(KEY_ACCESS_TOKEN, profileId), "")
            .putString(scopedKey(KEY_REFRESH_TOKEN, profileId), "")
            .putLong(scopedKey(KEY_EXPIRES_AT, profileId), 0L)
            .putString(scopedKey(KEY_USERNAME, profileId), "")
            .putLong(scopedKey(KEY_LAST_PULL, profileId), 0L)
            .apply()
    }

    /** Real-time /scrobble reporting from the player. Default on: it is the thing people
     *  connect Trakt for, and it does nothing at all until they sign in. */
    fun isScrobbleEnabled(prefs: SharedPreferences, profileId: Int): Boolean =
        getBoolean(prefs, KEY_SCROBBLE, profileId, true)

    fun setScrobbleEnabled(prefs: SharedPreferences, profileId: Int, enabled: Boolean) {
        prefs.edit().putBoolean(scopedKey(KEY_SCROBBLE, profileId), enabled).apply()
    }

    /** History writes plus the startup pull into WatchedStore. Default off: it writes to the
     *  cross-provider watched state, which is a bigger thing to turn on behind someone's back
     *  than a scrobble that only ever adds to a Trakt profile. */
    fun isWatchedSyncEnabled(prefs: SharedPreferences, profileId: Int): Boolean =
        getBoolean(prefs, KEY_WATCHED_SYNC, profileId, false)

    fun setWatchedSyncEnabled(prefs: SharedPreferences, profileId: Int, enabled: Boolean) {
        prefs.edit().putBoolean(scopedKey(KEY_WATCHED_SYNC, profileId), enabled).apply()
    }

    // ── TMDB id memo (backfill) ─────────────────

    private const val KEY_ID_MEMO = "trakt_tmdb_id_memo"
    /** Entries are short; the ceiling exists to bound the pref, not to curate it. A library
     *  big enough to hit this is far past the point where the backfill has already pushed
     *  everything that resolves. */
    private const val ID_MEMO_MAX = 8_000

    /**
     * Remembers what a normalised title resolved to on TMDB, so the backfill doesn't re-search
     * the same show on every run.
     *
     * Stored as `type|normalised|id` lines in one string set. A **negative** id is a memoised
     * "TMDB has never heard of this" - kept deliberately, because without it every run would
     * spend its whole budget re-searching the same handful of titles that will never match.
     */
    fun idMemo(prefs: SharedPreferences, profileId: Int): Map<String, Int> {
        val raw = getStringSet(prefs, KEY_ID_MEMO, profileId) ?: return emptyMap()
        val out = HashMap<String, Int>(raw.size)
        for (line in raw) {
            val cut = line.lastIndexOf('|')
            if (cut <= 0) continue
            val id = line.substring(cut + 1).toIntOrNull() ?: continue
            out[line.substring(0, cut)] = id
        }
        return out
    }

    fun saveIdMemo(prefs: SharedPreferences, profileId: Int, memo: Map<String, Int>) {
        val trimmed = if (memo.size <= ID_MEMO_MAX) memo else memo.entries.take(ID_MEMO_MAX).associate { it.toPair() }
        prefs.edit()
            .putStringSet(scopedKey(KEY_ID_MEMO, profileId), trimmed.map { (key, id) -> "$key|$id" }.toSet())
            .apply()
    }

    fun lastWatchedPullMs(prefs: SharedPreferences, profileId: Int): Long =
        getLong(prefs, KEY_LAST_PULL, profileId, 0L)

    fun markWatchedPulled(prefs: SharedPreferences, profileId: Int) {
        prefs.edit().putLong(scopedKey(KEY_LAST_PULL, profileId), System.currentTimeMillis()).apply()
    }

    /** Pure ownership check shared by asynchronous callers before applying a local effect. */
    fun isCurrentProfile(ownerProfileId: Int, activeProfileId: Int): Boolean =
        normalizedProfileId(ownerProfileId) == normalizedProfileId(activeProfileId)

    private fun getString(prefs: SharedPreferences, key: String, profileId: Int): String? {
        val scopedKey = scopedKey(key, profileId)
        return when {
            prefs.contains(scopedKey) -> prefs.getString(scopedKey, null)
            normalizedProfileId(profileId) == LEGACY_PROFILE_ID -> prefs.getString(key, null)
            else -> null
        }
    }

    private fun getBoolean(prefs: SharedPreferences, key: String, profileId: Int, defaultValue: Boolean): Boolean {
        val scopedKey = scopedKey(key, profileId)
        return when {
            prefs.contains(scopedKey) -> prefs.getBoolean(scopedKey, defaultValue)
            normalizedProfileId(profileId) == LEGACY_PROFILE_ID -> prefs.getBoolean(key, defaultValue)
            else -> defaultValue
        }
    }

    private fun getLong(prefs: SharedPreferences, key: String, profileId: Int, defaultValue: Long): Long {
        val scopedKey = scopedKey(key, profileId)
        return when {
            prefs.contains(scopedKey) -> prefs.getLong(scopedKey, defaultValue)
            normalizedProfileId(profileId) == LEGACY_PROFILE_ID -> prefs.getLong(key, defaultValue)
            else -> defaultValue
        }
    }

    private fun getStringSet(prefs: SharedPreferences, key: String, profileId: Int): Set<String>? {
        val scopedKey = scopedKey(key, profileId)
        return when {
            prefs.contains(scopedKey) -> prefs.getStringSet(scopedKey, null)?.toSet()
            normalizedProfileId(profileId) == LEGACY_PROFILE_ID -> prefs.getStringSet(key, null)?.toSet()
            else -> null
        }
    }

    private fun scopedKey(key: String, profileId: Int): String =
        "${key}_profile_${normalizedProfileId(profileId)}"

    private fun normalizedProfileId(profileId: Int): Int =
        profileId.takeIf { it > 0 } ?: LEGACY_PROFILE_ID

    private const val LEGACY_PROFILE_ID = 1
}
