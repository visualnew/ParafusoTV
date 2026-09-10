package com.lumora.presentation

import com.lumora.util.DeviceFormFactor

/**
 * Presentation boundary for the current host.
 *
 * The first Phase 2 step owns only the installation of existing chrome. Navigation, binding,
 * and all content rendering remain with MainActivity while the legacy View UI is migrated.
 */
internal interface PresentationShell {
    fun installLegacyChrome()
    fun renderLegacyNavigationSelection()
}

internal class TvPresentationShell(
    private val installTvLegacyChrome: () -> Unit,
    private val renderTvLegacyNavigationSelection: () -> Unit
) : PresentationShell {
    override fun installLegacyChrome() = installTvLegacyChrome()

    override fun renderLegacyNavigationSelection() = renderTvLegacyNavigationSelection()
}

internal class MobilePresentationShell(
    private val installMobileLegacyChrome: () -> Unit,
    private val renderMobileNavigationSelection: () -> Unit
) : PresentationShell {
    override fun installLegacyChrome() = installMobileLegacyChrome()

    /** Mobile owns a separate, touch-first navigation surface. */
    override fun renderLegacyNavigationSelection() = renderMobileNavigationSelection()
}

/** Selects the presentation shell exclusively from the already-resolved device form factor. */
internal fun presentationShellFor(
    formFactor: DeviceFormFactor,
    installTvLegacyChrome: () -> Unit,
    renderTvLegacyNavigationSelection: () -> Unit,
    installMobileLegacyChrome: () -> Unit,
    renderMobileNavigationSelection: () -> Unit = {}
): PresentationShell = when (formFactor) {
    DeviceFormFactor.TV -> TvPresentationShell(
        installTvLegacyChrome,
        renderTvLegacyNavigationSelection
    )
    DeviceFormFactor.MOBILE -> MobilePresentationShell(
        installMobileLegacyChrome,
        renderMobileNavigationSelection
    )
}
