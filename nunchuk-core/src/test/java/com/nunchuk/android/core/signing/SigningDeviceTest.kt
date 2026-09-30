package com.nunchuk.android.core.signing

import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import org.junit.Assert.assertEquals
import org.junit.Test

class SigningDeviceTest {

    @Test
    fun `signer type decides for every type but AIRGAP and HARDWARE, even with a COLDCARD tag`() {
        val expected = mapOf(
            SignerType.NFC to SigningDevice.TAPSIGNER,
            SignerType.COLDCARD_NFC to SigningDevice.COLDCARD,
            SignerType.PORTAL_NFC to SigningDevice.PORTAL,
            SignerType.SOFTWARE to SigningDevice.SOFTWARE,
            SignerType.FOREIGN_SOFTWARE to SigningDevice.FOREIGN_SOFTWARE,
            SignerType.SERVER to SigningDevice.SERVER,
            SignerType.PLATFORM to SigningDevice.PLATFORM,
            SignerType.UNKNOWN to SigningDevice.UNKNOWN,
        )
        expected.forEach { (type, device) ->
            assertEquals("$type", device, signingDevice(type, emptyList()))
            assertEquals("$type + COLDCARD", device, signingDevice(type, listOf(SignerTag.COLDCARD)))
            assertEquals(
                "$type + INHERITANCE, COLDCARD",
                device,
                signingDevice(type, listOf(SignerTag.INHERITANCE, SignerTag.COLDCARD)),
            )
        }
    }

    @Test
    fun `every signer type is classified`() {
        SignerType.entries.forEach { type ->
            signingDevice(type, emptyList())
            SignerTag.entries.forEach { tag -> signingDevice(type, listOf(tag)) }
        }
    }

    @Test
    fun `air-gapped keys follow their device tag`() {
        val expected = mapOf(
            SignerTag.JADE to SigningDevice.JADE,
            SignerTag.KEYSTONE to SigningDevice.KEYSTONE,
            SignerTag.SEEDSIGNER to SigningDevice.SEEDSIGNER,
            SignerTag.PASSPORT to SigningDevice.PASSPORT,
            SignerTag.KRUX to SigningDevice.KRUX,
            SignerTag.COLDCARD to SigningDevice.COLDCARD,
        )
        expected.forEach { (tag, device) -> assertAirgap(device, tag) }
    }

    @Test
    fun `an air-gapped key with no device tag is generic`() {
        assertEquals(SigningDevice.GENERIC_AIRGAP, signingDevice(SignerType.AIRGAP, emptyList()))
        assertEquals(
            SigningDevice.GENERIC_AIRGAP,
            signingDevice(SignerType.AIRGAP, listOf(SignerTag.INHERITANCE)),
        )
    }

    @Test
    fun `hardware keys follow their device tag`() {
        val expected = mapOf(
            SignerTag.LEDGER to SigningDevice.LEDGER,
            SignerTag.BITBOX to SigningDevice.BITBOX,
            SignerTag.TREZOR to SigningDevice.TREZOR,
            SignerTag.COLDCARD to SigningDevice.COLDCARD,
            SignerTag.KEEPKEY to SigningDevice.OTHER_HARDWARE,
        )
        expected.forEach { (tag, device) ->
            assertEquals("$tag", device, signingDevice(SignerType.HARDWARE, listOf(tag)))
            assertEquals(
                "INHERITANCE, $tag",
                device,
                signingDevice(SignerType.HARDWARE, listOf(SignerTag.INHERITANCE, tag)),
            )
        }
        assertEquals(SigningDevice.OTHER_HARDWARE, signingDevice(SignerType.HARDWARE, emptyList()))
    }

    @Test
    fun `a hardware key's own tag wins over the COLDCARD tag old inheritance uploads added`() {
        listOf(
            SignerTag.LEDGER to SigningDevice.LEDGER,
            SignerTag.BITBOX to SigningDevice.BITBOX,
            SignerTag.TREZOR to SigningDevice.TREZOR,
        ).forEach { (tag, device) ->
            assertEquals(
                device,
                signingDevice(SignerType.HARDWARE, listOf(SignerTag.INHERITANCE, SignerTag.COLDCARD, tag)),
            )
        }
    }

    /** The INHERITANCE tag says nothing about the device, whichever side of the device tag it sits. */
    private fun assertAirgap(device: SigningDevice, tag: SignerTag) {
        assertEquals("$tag", device, signingDevice(SignerType.AIRGAP, listOf(tag)))
        assertEquals("INHERITANCE, $tag", device, signingDevice(SignerType.AIRGAP, listOf(SignerTag.INHERITANCE, tag)))
        assertEquals("$tag, INHERITANCE", device, signingDevice(SignerType.AIRGAP, listOf(tag, SignerTag.INHERITANCE)))
    }
}
