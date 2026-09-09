package com.lumora.state

/**
 * In-memory intent for the legacy application status row.
 *
 * Rendering remains with the current host: whether this request can be shown still depends on
 * its player and overlay state. The text convention intentionally mirrors the existing UI:
 * messages ending in three ASCII dots after trailing whitespace are in progress.
 */
class StatusState {
    var statusText: String = ""
        private set
    var statusWanted: Boolean = false
        private set

    val isLoading: Boolean
        get() = statusText.trimEnd().endsWith("...")

    fun update(text: String, wanted: Boolean) {
        statusText = text
        statusWanted = wanted
    }
}
