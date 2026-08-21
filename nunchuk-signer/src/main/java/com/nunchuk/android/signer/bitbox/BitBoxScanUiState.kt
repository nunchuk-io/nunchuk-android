package com.nunchuk.android.signer.bitbox

import com.nunchuk.android.core.bitbox.BitBoxDevice
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.ResultExistingKey

/** UI state for the BitBox add-key flow, owned by [BitBoxViewModel]. */
data class BitBoxScanUiState(
    val isScanning: Boolean = false,
    val devices: List<BitBoxDevice> = emptyList(),
    val selectedAddress: String? = null,
    val statusText: String = "",
    val isProcessing: Boolean = false,
    /**
     * Transport the user picked on the intro screen. Kept so a rescan — including "Scan again"
     * on the BitBoxApp screen — goes back over the same transport instead of always asking for
     * the Bluetooth permission.
     */
    val isUsbTransport: Boolean = false,
    // Wallet config chosen on screen 03; drives get-xpub + createSigner.
    val walletType: WalletType = WalletType.SINGLE_SIG,
    val addressType: AddressType = AddressType.NATIVE_SEGWIT,
    val accountIndex: Int = 0,
    /** Shown on the confirm-pairing screen; only set for a pairing this account hasn't seen. */
    val pairingCode: String = "",
    // BitBox reads the xpub at an explicit derivation path (Ledger derives it from the wallet
    // type itself), so the path is resolved between the fingerprint and xpub round trips and
    // held here for the createSigner that follows.
    val masterFingerprint: String = "",
    val derivationPath: String = "",
    // Set once the xpub is fetched: the signer waits on the name step (standalone flow) and/or
    // the replace-key confirmation before being persisted.
    val pendingSigner: SingleSigner? = null,
    val existingKeyType: ResultExistingKey? = null,
    val replaceExistingKey: Boolean = false,
    // Prefilled into the "Name your key" field; the connected device name when we have one.
    val defaultSignerName: String = "",
) {
    val selectedDevice: BitBoxDevice?
        get() = devices.firstOrNull { it.id == selectedAddress }
}
