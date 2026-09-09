package com.lumora

import android.content.Context
import java.io.File

/** Central namespace for data that belongs to the currently selected ParafusoTV profile. */
object ParafusoProfileScope {
    private const val PREFS = "parafuso_profiles"
    private const val ACTIVE_ID = "active_id"
    private const val DEFAULT_ID = 1

    fun activeId(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(ACTIVE_ID, DEFAULT_ID).takeIf { it > 0 } ?: DEFAULT_ID

    fun key(base: String, context: Context): String = "${base}_profile_${activeId(context)}"

    fun file(context: Context, base: String): File = File(context.filesDir, "${base}_profile_${activeId(context)}")
}
