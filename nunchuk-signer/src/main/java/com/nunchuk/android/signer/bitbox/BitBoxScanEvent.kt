package com.nunchuk.android.signer.bitbox

import com.nunchuk.android.model.SingleSigner

/** One-shot events emitted by [BitBoxViewModel] for the BitBox add-key flow to act on. */
sealed class BitBoxScanEvent {

    /**
     * Initialization cleared every gate — the device is genuine, its firmware is usable and it
     * has been set up — so read its master fingerprint next.
     */
    data object ReadMasterFingerprint : BitBoxScanEvent()

    /** The derivation path is resolved; read the xpub at it. */
    data class FetchXpub(val derivationPath: String) : BitBoxScanEvent()

    /** A new pairing needs the code comparing — design screen 05. */
    data class NavigateToConfirmPairing(val pairingCode: String) : BitBoxScanEvent()

    /** Uninitialized / upgrade-only / bootloader — design screen 05B. */
    data object NavigateToBitBoxAppRequired : BitBoxScanEvent()

    /** Attestation came back INVALID — design screen 05C. */
    data object NavigateToAttestationWarning : BitBoxScanEvent()

    /** Standalone add-key: let the user name the key — design screen 06. */
    data object NavigateToSetKeyName : BitBoxScanEvent()

    data class OpenSignerInfo(val signer: SingleSigner) : BitBoxScanEvent()

    data class Error(val message: String) : BitBoxScanEvent()
}
