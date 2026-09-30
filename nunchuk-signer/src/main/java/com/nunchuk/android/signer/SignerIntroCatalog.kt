package com.nunchuk.android.signer

import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType

private fun multiSigSigner(type: SignerType, tag: SignerTag? = null) = SupportedSigner(
    type = type,
    tag = tag,
    walletType = WalletType.MULTI_SIG,
    addressType = AddressType.NATIVE_SEGWIT
)

/**
 * Key types offered when adding the inheritance key while setting up an off-chain timelock
 * wallet. Hardware only — an inheritance key must live outside the owner's phone, so there is no
 * software key here. Portal is left out: it is not offered as an inheritance key today. The order
 * is the picker order in the design.
 */
internal val offChainInheritanceSetupKeyTypes = listOf(
    multiSigSigner(SignerType.NFC),
    multiSigSigner(SignerType.HARDWARE, SignerTag.TREZOR),
    multiSigSigner(SignerType.AIRGAP, SignerTag.JADE),
    multiSigSigner(SignerType.AIRGAP, SignerTag.SEEDSIGNER),
    multiSigSigner(SignerType.AIRGAP, SignerTag.KEYSTONE),
    multiSigSigner(SignerType.AIRGAP, SignerTag.PASSPORT),
    multiSigSigner(SignerType.COLDCARD_NFC),
    multiSigSigner(SignerType.HARDWARE, SignerTag.BITBOX),
    multiSigSigner(SignerType.HARDWARE, SignerTag.LEDGER),
    multiSigSigner(SignerType.AIRGAP, SignerTag.KRUX),
    // Carries no card of its own (toDisplayInfo returns null for an untagged air-gap); it is what
    // enables the "Generic Airgap" row.
    multiSigSigner(SignerType.AIRGAP),
)

/**
 * Card order of the BYOH picker, as laid out in the design. Derived from
 * [offChainInheritanceSetupKeyTypes] so that list stays the one place the order is written down.
 * The untagged air-gap entry drops out here (no card of its own), leaving only the grid items.
 */
internal val offChainInheritanceCardOrder: List<KeyType> =
    offChainInheritanceSetupKeyTypes.mapNotNull { it.toKeyType() }

/**
 * Fallback key types offered to a Beneficiary claiming an off-chain inheritance, in the picker order
 * of the design, until the server's inheritance-key list for the wallet type is available.
 * Only devices the claim flow can take a challenge signature from belong here: TAPSIGNER and
 * Coldcard via NFC, Jade, Keystone and SeedSigner via plain-text QR, Passport via a microSD file,
 * Krux via SD card or QR, Ledger and BitBox in-app over BLE/USB, Trezor through Trezor Suite (see
 * VerifyInheritanceMessageScreen). The software key stays — the Beneficiary may
 * hold nothing but the seed phrase.
 */
internal val offChainInheritanceClaimKeyTypes = listOf(
    multiSigSigner(SignerType.NFC),
    multiSigSigner(SignerType.HARDWARE, SignerTag.TREZOR),
    multiSigSigner(SignerType.AIRGAP, SignerTag.JADE),
    multiSigSigner(SignerType.AIRGAP, SignerTag.SEEDSIGNER),
    multiSigSigner(SignerType.AIRGAP, SignerTag.KEYSTONE),
    multiSigSigner(SignerType.AIRGAP, SignerTag.PASSPORT),
    multiSigSigner(SignerType.COLDCARD_NFC),
    multiSigSigner(SignerType.HARDWARE, SignerTag.BITBOX),
    multiSigSigner(SignerType.HARDWARE, SignerTag.LEDGER),
    multiSigSigner(SignerType.AIRGAP, SignerTag.KRUX),
    multiSigSigner(SignerType.SOFTWARE),
)

/** Card order of the claim picker, derived like [offChainInheritanceCardOrder]. */
internal val offChainInheritanceClaimCardOrder: List<KeyType> =
    offChainInheritanceClaimKeyTypes.mapNotNull { it.toKeyType() }

val defaultSupportedSigners = listOf(
    SupportedSigner(
        type = SignerType.NFC,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.HARDWARE,
        tag = SignerTag.TREZOR,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.JADE,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.PORTAL_NFC,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.SEEDSIGNER,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.KEYSTONE,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.PASSPORT,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.COLDCARD_NFC,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.HARDWARE,
        tag = SignerTag.BITBOX,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.HARDWARE,
        tag = SignerTag.LEDGER,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.KRUX,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.SOFTWARE,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    )
)
