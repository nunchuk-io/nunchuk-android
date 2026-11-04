package com.nunchuk.android.core.network

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SanitizeDeviceNameTest {

    @Test
    fun `trims a normal model name`() {
        assertEquals("Pixel 8 Pro", sanitizeDeviceName("  Pixel 8 Pro "))
    }

    @Test
    fun `omits empty or blank names`() {
        assertNull(sanitizeDeviceName(null))
        assertNull(sanitizeDeviceName(""))
        assertNull(sanitizeDeviceName("   "))
    }

    @Test
    fun `omits names longer than 100 characters`() {
        assertEquals("a".repeat(100), sanitizeDeviceName("a".repeat(100)))
        assertNull(sanitizeDeviceName("a".repeat(101)))
    }

    @Test
    fun `omits control characters and non ascii`() {
        assertNull(sanitizeDeviceName("Pixel\n8"))
        assertNull(sanitizeDeviceName("Pixel\u00008"))
        assertNull(sanitizeDeviceName("Galaxy S24 é"))
        assertNull(sanitizeDeviceName("小米 14"))
    }

    @Test
    fun `accepted values are valid okhttp header values`() {
        val value = sanitizeDeviceName("SM-S928B/DS (v2)")!!
        Request.Builder().url("https://example.com").header(ApiConstant.HEADER_DEVICE_NAME, value).build()
    }

    @Test
    fun `invalid name does not add the header`() {
        val request = Request.Builder().url("https://example.com")
            .validHeader(ApiConstant.HEADER_DEVICE_NAME, sanitizeDeviceName("bad\r\nname"))
            .build()
        assertNull(request.header(ApiConstant.HEADER_DEVICE_NAME))
    }
}
