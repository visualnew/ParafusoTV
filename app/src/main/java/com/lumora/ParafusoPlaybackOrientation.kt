package com.lumora

import android.app.Activity
import android.content.pm.ActivityInfo
import com.lumora.util.isTvDevice

/**
 * ParafusoTV playback orientation policy.
 *
 * Phones browse in the user's preferred orientation, but fullscreen video is presented in
 * landscape. TV devices are never forced to rotate. The previous phone orientation is restored
 * when playback closes, so a portrait user returns to the catalog exactly as they left it.
 */
internal var MainActivity.playbackPreviousOrientation: Int?
    get() = window.decorView.getTag(R.id.playback_previous_orientation_tag) as? Int
    set(value) { window.decorView.setTag(R.id.playback_previous_orientation_tag, value) }

internal fun MainActivity.enterParafusoPlaybackOrientation() {
    if (isTvDevice(this)) return
    if (playbackPreviousOrientation != null) return
    playbackPreviousOrientation = requestedOrientation
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}

internal fun MainActivity.exitParafusoPlaybackOrientation() {
    if (isTvDevice(this)) return
    val previous = playbackPreviousOrientation ?: return
    requestedOrientation = previous
    playbackPreviousOrientation = null
}
