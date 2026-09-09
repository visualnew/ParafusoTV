package com.lumora.presentation

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import java.lang.ref.WeakReference

/** Ephemeral presentation state for the legacy TV dashboard rail. */
internal enum class TvDashboardRailState {
    COLLAPSED,
    EXPANDED
}

/**
 * Keeps the existing TV rail compact until a rail item owns focus.
 *
 * The controller deliberately knows nothing about navigation destinations, providers, or
 * player state. Callers provide the current browsing surface's focus targets.
 */
internal class TvDashboardRailController(
    private val rail: ViewGroup,
    private val mainContent: View,
    private val logo: ImageView,
    private val labels: List<View>,
    private val items: List<View>,
    private val collapsedWidth: Int,
    private val expandedWidth: Int,
    private val collapsedLogoSize: Int,
    private val expandedLogoSize: Int,
    private val canEnterFromContent: () -> Boolean,
    private val focusCurrentContent: () -> Unit
) {
    private var lastContentFocus: WeakReference<View>? = null
    var state: TvDashboardRailState = TvDashboardRailState.COLLAPSED
        private set

    fun install() {
        items.forEach { item ->
            item.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    setState(TvDashboardRailState.EXPANDED)
                } else {
                    // Focus briefly leaves one row before it reaches the next one. Waiting one
                    // loop turn avoids collapsing/re-expanding while moving vertically.
                    rail.post {
                        if (!rail.hasFocus()) setState(TvDashboardRailState.COLLAPSED)
                    }
                }
            }
            item.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    returnToContent()
                } else {
                    false
                }
            }
        }
        setState(TvDashboardRailState.COLLAPSED)
    }

    /** Enters the rail only from a caller that established a real content left edge. */
    fun enterFromContent(contentFocus: View, preferredRailItem: View): Boolean {
        if (!canEnterFromContent()) return false
        lastContentFocus = WeakReference(contentFocus)
        setState(TvDashboardRailState.EXPANDED)
        if (preferredRailItem.requestFocus()) return true
        // A failed request must not leave an unfocused rail occupying expanded width.
        setState(TvDashboardRailState.COLLAPSED)
        return false
    }

    /**
     * Returns to the exact content view when it remains on screen. If a catalog update
     * detached it, the caller's current-surface fallback is the safe destination.
     */
    private fun returnToContent(): Boolean {
        if (!canEnterFromContent()) return false
        val previous = lastContentFocus?.get()
        if (previous != null && previous.isShown && previous.isFocusable && previous.requestFocus()) {
            return true
        }
        focusCurrentContent()
        return true
    }

    private fun setState(next: TvDashboardRailState) {
        if (state == next && rail.layoutParams.width == widthFor(next)) return
        state = next
        val expanded = next == TvDashboardRailState.EXPANDED
        val width = widthFor(next)
        rail.layoutParams = rail.layoutParams.also { it.width = width }
        labels.forEach { it.visibility = if (expanded) View.VISIBLE else View.GONE }
        logo.layoutParams = logo.layoutParams.also {
            val size = if (expanded) expandedLogoSize else collapsedLogoSize
            it.width = size
            it.height = size
        }
        mainContent.setPaddingRelative(
            width,
            mainContent.paddingTop,
            mainContent.paddingEnd,
            mainContent.paddingBottom
        )
        rail.requestLayout()
        mainContent.requestLayout()
    }

    private fun widthFor(state: TvDashboardRailState): Int = when (state) {
        TvDashboardRailState.COLLAPSED -> collapsedWidth
        TvDashboardRailState.EXPANDED -> expandedWidth
    }
}
