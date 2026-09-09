package com.lumora.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceFormFactorTest {

    @Test
    fun `legacy television result resolves to TV`() {
        val formFactor = DeviceFormFactor.fromIsTvDevice(isTvDevice = true)

        assertTrue(formFactor.isTv)
    }

    @Test
    fun `legacy non television result resolves to MOBILE`() {
        val formFactor = DeviceFormFactor.fromIsTvDevice(isTvDevice = false)

        assertFalse(formFactor.isTv)
    }
}
