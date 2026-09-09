package com.lumora.state

import com.lumora.ParafusoParentalStore
import com.lumora.ParafusoProfile
import com.lumora.ParafusoProfileStore

/**
 * In-memory access boundary for the currently selected ParafusoTV profile.
 *
 * It deliberately delegates all persistence and activation behavior to the existing stores.
 * In particular, [activeId] preserves -1 before first selection; it never applies the
 * profile-data fallback used by ParafusoProfileScope.
 */
class ProfileSession(
    private val profileStore: ParafusoProfileStore,
    private val parentalStore: ParafusoParentalStore
) {
    fun profiles(): List<ParafusoProfile> = profileStore.profiles()

    fun activeId(): Int = profileStore.activeId()

    fun activeProfile(): ParafusoProfile? =
        profiles().firstOrNull { it.id == activeId() }

    fun isChild(): Boolean = activeProfile()?.child == true

    fun blockedCategories(): Set<String> {
        val profile = activeProfile()
        return if (profile?.child == true) parentalStore.blockedCategories(profile.id) else emptySet()
    }

    fun ensureDefault(): List<ParafusoProfile> = profileStore.ensureDefault()

    fun activate(id: Int) {
        profileStore.setActive(id)
    }
}
