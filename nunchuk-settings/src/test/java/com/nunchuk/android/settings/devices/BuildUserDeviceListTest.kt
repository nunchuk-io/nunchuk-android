package com.nunchuk.android.settings.devices

import com.nunchuk.android.core.profile.UserDeviceResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildUserDeviceListTest {

    private val olderPhone = UserDeviceResponse(id = "OLDMATRIX", name = "Old phone", lastTs = 100)
    private val browser = UserDeviceResponse(id = "BROWSER", name = "Chrome", lastTs = 300)
    private val thisPhoneMatrix = UserDeviceResponse(id = "MATRIXDEV", name = "Pixel", lastTs = 200)
    private val thisPhoneApi = UserDeviceResponse(id = "api-device-id", name = "Pixel", lastTs = 50)

    private fun List<UserDeviceUiModel>.current() = filter { it.isCurrentDevice }.map { it.device }

    @Test
    fun `newer browser login does not steal the current badge`() {
        val result = buildUserDeviceList(listOf(olderPhone, browser, thisPhoneMatrix), "MATRIXDEV", "api-device-id")
        assertEquals(listOf(thisPhoneMatrix), result.current())
        assertEquals(listOf(thisPhoneMatrix, browser, olderPhone), result.map { it.device })
    }

    @Test
    fun `no match marks no row current and keeps every row`() {
        val result = buildUserDeviceList(listOf(olderPhone, browser), "MATRIXDEV", "api-device-id")
        assertTrue(result.current().isEmpty())
        assertEquals(listOf(browser, olderPhone), result.map { it.device })
    }

    @Test
    fun `matrix disconnected falls back to api device id`() {
        val result = buildUserDeviceList(listOf(browser, thisPhoneApi, olderPhone), null, "api-device-id")
        assertEquals(listOf(thisPhoneApi), result.current())
        assertEquals(listOf(thisPhoneApi, browser, olderPhone), result.map { it.device })
    }

    @Test
    fun `both ids present marks only the matrix row and keeps both`() {
        val result = buildUserDeviceList(listOf(thisPhoneApi, browser, thisPhoneMatrix), "MATRIXDEV", "api-device-id")
        assertEquals(listOf(thisPhoneMatrix), result.current())
        assertEquals(3, result.size)
        assertEquals(thisPhoneMatrix, result.first().device)
    }

    @Test
    fun `blank candidate ids never match blank or missing row ids`() {
        val noId = UserDeviceResponse(id = null, name = "Unknown", lastTs = 10)
        val blankId = UserDeviceResponse(id = "", name = "Blank", lastTs = 20)
        val result = buildUserDeviceList(listOf(noId, blankId, browser), "", "  ")
        assertTrue(result.current().isEmpty())
        assertEquals(3, result.size)
        assertTrue(buildUserDeviceList(listOf(noId, blankId), null, "").current().isEmpty())
    }

    @Test
    fun `duplicate names are not merged`() {
        val twin = thisPhoneMatrix.copy(id = "OTHER", lastTs = 999)
        val result = buildUserDeviceList(listOf(thisPhoneMatrix, twin), "MATRIXDEV", null)
        assertEquals(listOf(thisPhoneMatrix, twin), result.map { it.device })
        assertEquals(listOf(thisPhoneMatrix), result.current())
    }
}
