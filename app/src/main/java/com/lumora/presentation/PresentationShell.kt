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
}

internal class TvPresentationShell(
    private val installTvLegacyChrome: () -> Unit
) : PresentationShell {
    override fun installLegacyChrome() = installTvLegacyChrome()
}

internal class MobilePresentationShell(
    private val installMobileLegacyChrome: () -> Unit
) : PresentationShell {
    override fun installLegacyChrome() = installMobileLegacyChrome()
}

/** Selects the presentation shell exclusively from the already-resolved device form factor. */
internal fun presentationShellFor(
    formFactor: DeviceFormFactor,
    installTvLegacyChrome: () -> Unit,
    installMobileLegacyChrome: () -> Unit
): PresentationShell = when (formFactor) {
    DeviceFormFactor.TV -> TvPresentationShell(installTvLegacyChrome)
    DeviceFormFactor.MOBILE -> MobilePresentationShell(installMobileLegacyChrome)
}
