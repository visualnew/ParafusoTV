package com.lumora.state

/** Canonical destinations shared by future presentation shells. */
enum class NavigationDestination {
    HOME,
    CATALOG,
    DISCOVER,
    DOWNLOADS,
    CATCHUP
}

/**
 * Navigation values used by the current Activity-based presentation.
 *
 * The booleans deliberately mirror the legacy fields for this migration step. Their public
 * setters preserve the old one-field-at-a-time semantics; new callers should use the atomic
 * navigation methods below.
 */
data class NavigationState(
    val activeTab: Int = 0,
    val showingHome: Boolean = false,
    val showingDownloads: Boolean = false,
    val showingDiscover: Boolean = false,
    val showingCatchup: Boolean = false
) {
    val destination: NavigationDestination
        get() = when {
            showingCatchup -> NavigationDestination.CATCHUP
            showingHome -> NavigationDestination.HOME
            showingDownloads -> NavigationDestination.DOWNLOADS
            showingDiscover -> NavigationDestination.DISCOVER
            else -> NavigationDestination.CATALOG
        }
}

/**
 * Single in-memory owner of navigation state.
 *
 * It has no Android or view dependency, so transitions can be verified with local unit tests.
 * Existing callers remain compatible through MainActivity's forwarding properties.
 */
class NavigationCoordinator(initialState: NavigationState = NavigationState()) {
    var state: NavigationState = initialState
        private set

    /** Compatibility mutator: preserves the former unconstrained Activity property. */
    fun setActiveTab(index: Int) {
        state = state.copy(activeTab = index)
    }

    /** Compatibility mutators: each changes only its original legacy flag. */
    fun setShowingHome(showing: Boolean) {
        state = state.copy(showingHome = showing)
    }

    fun setShowingDownloads(showing: Boolean) {
        state = state.copy(showingDownloads = showing)
    }

    fun setShowingDiscover(showing: Boolean) {
        state = state.copy(showingDiscover = showing)
    }

    fun setShowingCatchup(showing: Boolean) {
        state = state.copy(showingCatchup = showing)
    }

    fun showHome() {
        state = state.copy(
            showingHome = true,
            showingDownloads = false,
            showingDiscover = false,
            showingCatchup = false
        )
    }

    fun showCatalogTab(index: Int) {
        require(index in 0..2) { "Catalog tab must be Live (0), Series (1), or Films (2)." }
        state = state.copy(
            activeTab = index,
            showingHome = false,
            showingDownloads = false,
            showingDiscover = false,
            showingCatchup = false
        )
    }

    fun showDiscover() {
        state = state.copy(
            showingHome = false,
            showingDownloads = false,
            showingDiscover = true,
            showingCatchup = false
        )
    }

    fun showDownloads() {
        state = state.copy(
            showingHome = false,
            showingDownloads = true,
            showingDiscover = false,
            showingCatchup = false
        )
    }

    fun showCatchup() {
        state = state.copy(
            showingHome = false,
            showingDownloads = false,
            showingDiscover = false,
            showingCatchup = true
        )
    }
}
