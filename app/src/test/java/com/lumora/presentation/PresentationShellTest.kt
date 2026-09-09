package com.lumora.presentation

import com.lumora.util.DeviceFormFactor
import org.junit.Assert.assertEquals
import org.junit.Test

class PresentationShellTest {

    @Test
    fun `TV selection installs TV chrome and renders TV rail selection`() {
        var tvInstalls = 0
        var mobileInstalls = 0
        var tvSelectionRenders = 0

        presentationShellFor(
            formFactor = DeviceFormFactor.TV,
            installTvLegacyChrome = { tvInstalls++ },
            renderTvLegacyNavigationSelection = { tvSelectionRenders++ },
            installMobileLegacyChrome = { mobileInstalls++ }
        ).also { shell ->
            shell.installLegacyChrome()
            shell.renderLegacyNavigationSelection()
        }

        assertEquals(1, tvInstalls)
        assertEquals(0, mobileInstalls)
        assertEquals(1, tvSelectionRenders)
    }

    @Test
    fun `MOBILE selection installs mobile chrome without rendering a rail`() {
        var tvInstalls = 0
        var mobileInstalls = 0
        var tvSelectionRenders = 0

        presentationShellFor(
            formFactor = DeviceFormFactor.MOBILE,
            installTvLegacyChrome = { tvInstalls++ },
            renderTvLegacyNavigationSelection = { tvSelectionRenders++ },
            installMobileLegacyChrome = { mobileInstalls++ }
        ).also { shell ->
            shell.installLegacyChrome()
            shell.renderLegacyNavigationSelection()
        }

        assertEquals(0, tvInstalls)
        assertEquals(1, mobileInstalls)
        assertEquals(0, tvSelectionRenders)
    }
}
