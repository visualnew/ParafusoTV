package com.lumora.data

import android.content.SharedPreferences
import org.json.JSONObject

/** Small local metadata store for the ParafusoTV provider manager.
 *  Catalog content remains in ChannelCache; this only remembers per-provider state shown
 *  in Settings. Keeping it keyed by provider id is important: one account must never make
 *  another account look refreshed, authenticated, or expired. */
object IptvProviderMetaStore {
    private const val KEY = "parafusotv_provider_meta"
    private const val LEGACY_XTREAM_EXPIRY_KEY = "xtream_exp_date"
    private const val LEGACY_XTREAM_TRIAL_KEY = "xtream_is_trial"

    data class Meta(
        val refreshedAt: Long = 0L,
        val itemCount: Int = 0,
        val lastError: String? = null,
        val authenticationState: String? = null,
        val xtreamExpirySeconds: Long? = null,
        val xtreamIsTrial: Boolean = false
    ) {
        val authenticationFailed: Boolean get() = authenticationState == "failed"
    }

    @Synchronized
    fun get(prefs: SharedPreferences, providerId: String): Meta {
        val root = runCatching { JSONObject(prefs.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())
        val o = root.optJSONObject(providerId) ?: return Meta()
        return Meta(
            refreshedAt = o.optLong("refreshedAt", 0L),
            itemCount = o.optInt("itemCount", 0),
            lastError = o.optString("lastError").takeIf { it.isNotBlank() },
            authenticationState = o.optString("authenticationState").takeIf { it.isNotBlank() }
                ?: if (o.optBoolean("authenticationFailed", false)) "failed" else null,
            xtreamExpirySeconds = o.optString("xtreamExpirySeconds").toLongOrNull(),
            xtreamIsTrial = o.optBoolean("xtreamIsTrial", false)
        )
    }

    /** Records an actual catalog success only. A failure must use [saveFailure], preserving
     *  both this timestamp and count from the last known-good catalog. */
    @Synchronized
    fun saveSuccess(
        prefs: SharedPreferences,
        providerId: String,
        itemCount: Int,
        refreshedAt: Long = System.currentTimeMillis(),
        xtreamExpirySeconds: Long? = null,
        xtreamIsTrial: Boolean = false,
        xtreamAuthenticated: Boolean = false
    ) {
        val root = runCatching { JSONObject(prefs.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())
        val previous = root.optJSONObject(providerId)
        root.put(providerId, JSONObject(previous?.toString() ?: "{}").apply {
            put("refreshedAt", refreshedAt)
            put("itemCount", itemCount.coerceAtLeast(0))
            remove("lastError")
            remove("authenticationFailed")
            if (xtreamAuthenticated) put("authenticationState", "authenticated") else remove("authenticationState")
            if (xtreamExpirySeconds != null) put("xtreamExpirySeconds", xtreamExpirySeconds.toString())
            else remove("xtreamExpirySeconds")
            put("xtreamIsTrial", xtreamIsTrial)
        })
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    /** Stores a useful last error without turning a failed attempt into a successful refresh. */
    @Synchronized
    fun saveFailure(
        prefs: SharedPreferences,
        providerId: String,
        error: String,
        authenticationFailed: Boolean = false,
        xtreamExpirySeconds: Long? = null,
        xtreamIsTrial: Boolean = false,
        xtreamAuthenticated: Boolean = false
    ) {
        val root = runCatching { JSONObject(prefs.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())
        val previous = root.optJSONObject(providerId)
        root.put(providerId, JSONObject(previous?.toString() ?: "{}").apply {
            put("lastError", error.take(120))
            remove("authenticationFailed")
            when {
                authenticationFailed -> put("authenticationState", "failed")
                xtreamAuthenticated -> put("authenticationState", "authenticated")
            }
            if (xtreamAuthenticated) {
                if (xtreamExpirySeconds != null) put("xtreamExpirySeconds", xtreamExpirySeconds.toString())
                else remove("xtreamExpirySeconds")
                put("xtreamIsTrial", xtreamIsTrial)
            }
        })
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    /** Imports the old global Xtream fields only when one configured Xtream account makes
     *  their ownership unambiguous. With multiple accounts the legacy value is deliberately
     *  left untouched instead of assigning one account's expiry to another. */
    @Synchronized
    fun migrateLegacyXtreamStatus(prefs: SharedPreferences, xtreamProviderIds: List<String>) {
        if (xtreamProviderIds.size != 1) return
        val providerId = xtreamProviderIds.single()
        val current = get(prefs, providerId)
        if (current.xtreamExpirySeconds != null || current.authenticationState != null) return
        val expiry = prefs.getString(LEGACY_XTREAM_EXPIRY_KEY, null)?.toLongOrNull() ?: return
        val root = runCatching { JSONObject(prefs.getString(KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())
        val entry = JSONObject(root.optJSONObject(providerId)?.toString() ?: "{}").apply {
            put("xtreamExpirySeconds", expiry.toString())
            put("xtreamIsTrial", prefs.getBoolean(LEGACY_XTREAM_TRIAL_KEY, false))
        }
        root.put(providerId, entry)
        prefs.edit().putString(KEY, root.toString()).apply()
    }
}
