package com.nunchuk.android.core.signing

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

/** Claim signing identity, independent of the add-key picker and other signing flows. */
enum class SigningDevice {
    SOFTWARE, FOREIGN_SOFTWARE, TAPSIGNER, PORTAL, COLDCARD, JADE, KEYSTONE, SEEDSIGNER,
    PASSPORT, KRUX, GENERIC_AIRGAP, LEDGER, BITBOX, TREZOR, OTHER_HARDWARE, SERVER, PLATFORM, UNKNOWN,
}

fun SignerModel.signingDevice(): SigningDevice = signingDevice(type, tags)

/**
 * Signer type first; tags are read only for AIRGAP and HARDWARE. A key carries one device tag, plus
 * INHERITANCE on an inheritance key, which says nothing about the device. The device tags are checked
 * ahead of COLDCARD, which old inheritance-key uploads added next to a HARDWARE key's own tag.
 */
fun signingDevice(type: SignerType, tags: List<SignerTag>): SigningDevice = when (type) {
    SignerType.NFC -> SigningDevice.TAPSIGNER
    SignerType.COLDCARD_NFC -> SigningDevice.COLDCARD
    SignerType.PORTAL_NFC -> SigningDevice.PORTAL
    SignerType.SOFTWARE -> SigningDevice.SOFTWARE
    SignerType.FOREIGN_SOFTWARE -> SigningDevice.FOREIGN_SOFTWARE
    SignerType.SERVER -> SigningDevice.SERVER
    SignerType.PLATFORM -> SigningDevice.PLATFORM
    SignerType.UNKNOWN -> SigningDevice.UNKNOWN
    SignerType.AIRGAP -> when {
        SignerTag.JADE in tags -> SigningDevice.JADE
        SignerTag.KEYSTONE in tags -> SigningDevice.KEYSTONE
        SignerTag.SEEDSIGNER in tags -> SigningDevice.SEEDSIGNER
        SignerTag.PASSPORT in tags -> SigningDevice.PASSPORT
        SignerTag.KRUX in tags -> SigningDevice.KRUX
        SignerTag.COLDCARD in tags -> SigningDevice.COLDCARD
        else -> SigningDevice.GENERIC_AIRGAP
    }
    SignerType.HARDWARE -> when {
        SignerTag.LEDGER in tags -> SigningDevice.LEDGER
        SignerTag.BITBOX in tags -> SigningDevice.BITBOX
        SignerTag.TREZOR in tags -> SigningDevice.TREZOR
        SignerTag.COLDCARD in tags -> SigningDevice.COLDCARD
        else -> SigningDevice.OTHER_HARDWARE
    }
}
