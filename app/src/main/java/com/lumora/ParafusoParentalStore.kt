package com.lumora

import android.content.Context
import java.security.MessageDigest

/** Local parental-control policy, scoped to each ParafusoTV profile. */
class ParafusoParentalStore(context: Context) {
    private val prefs = context.getSharedPreferences("parafuso_parental", Context.MODE_PRIVATE)

    private fun pinKey(profileId: Int) = "pin_profile_$profileId"
    private fun blockedKey(profileId: Int) = "blocked_profile_$profileId"

    fun hasPin(profileId: Int): Boolean = !prefs.getString(pinKey(profileId), null).isNullOrBlank()

    fun setPin(profileId: Int, pin: String?) {
        val value = pin?.trim().orEmpty()
        prefs.edit().apply {
            if (value.isBlank()) remove(pinKey(profileId)) else putString(pinKey(profileId), hash(value))
        }.apply()
    }

    fun verifyPin(profileId: Int, pin: String): Boolean =
        prefs.getString(pinKey(profileId), null) == hash(pin.trim())

    fun blockedCategories(profileId: Int): Set<String> =
        prefs.getStringSet(blockedKey(profileId), emptySet())?.toSet() ?: emptySet()

    fun setBlockedCategories(profileId: Int, categories: Set<String>) {
        prefs.edit().putStringSet(blockedKey(profileId), categories).apply()
    }

    fun isBlocked(profileId: Int, categoryName: String?): Boolean {
        if (categoryName.isNullOrBlank()) return false
        val blocked = blockedCategories(profileId)
        return categoryName in blocked || categoryName.trim().lowercase() in blocked.map { it.lowercase() }
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
