package com.lumora.presentation

import com.lumora.util.DeviceFormFactor
import org.junit.Assert.assertEquals
import org.junit.Test

class PresentationShellTest {

    @Test
    fun `TV selection installs only the TV legacy chrome`() {
        var tvInstalls = 0
        var mobileInstalls = 0

        presentationShellFor(
            formFactor = DeviceFormFactor.TV,
            installTvLegacyChrome = { tvInstalls++ },
            installMobileLegacyChrome = { mobileInstalls++ }
        ).installLegacyChrome()

        assertEquals(1, tvInstalls)
        assertEquals(0, mobileInstalls)
    }

    @Test
    fun `MOBILE selection installs only the mobile legacy chrome`() {
        var tvInstalls = 0
        var mobileInstalls = 0

        presentationShellFor(
            formFactor = DeviceFormFactor.MOBILE,
            installTvLegacyChrome = { tvInstalls++ },
            installMobileLegacyChrome = { mobileInstalls++ }
        ).installLegacyChrome()

        assertEquals(0, tvInstalls)
        assertEquals(1, mobileInstalls)
    }
}
