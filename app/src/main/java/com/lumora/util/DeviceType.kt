package com.lumora.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

/**
 * Presentation form factor selected from the existing TV-device detection.
 *
 * This deliberately has no orientation or screen-size distinction: those presentation concerns
 * remain outside this first Phase 2 boundary.
 */
enum class DeviceFormFactor {
    TV,
    MOBILE;

    val isTv: Boolean
        get() = this == TV

    companion object {
        fun fromIsTvDevice(isTvDevice: Boolean): DeviceFormFactor =
            if (isTvDevice) TV else MOBILE
    }
}

/** Downloads only make sense on a device with local storage the user actually browses - not a TV box. */
fun isTvDevice(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

/**
 * Reusable form-factor resolver. It intentionally delegates to [isTvDevice] so Android TV and
 * Fire TV retain precisely the current UiModeManager-based classification.
 */
fun resolveDeviceFormFactor(context: Context): DeviceFormFactor =
    DeviceFormFactor.fromIsTvDevice(isTvDevice(context))
