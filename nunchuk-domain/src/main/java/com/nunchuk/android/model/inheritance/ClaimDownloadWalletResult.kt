package com.nunchuk.android.model.inheritance

import com.nunchuk.android.model.BufferPeriodCountdown
import com.nunchuk.android.model.KeyOrigin
import com.nunchuk.android.model.WalletServer

/** Outcome of `claiming/download-wallet` for the keys submitted so far (NUN-10320 §7). */
sealed interface ClaimDownloadWalletResult {
    /** A claim condition is complete; the wallet is stored locally. */
    data class Downloaded(val wallet: WalletServer) : ClaimDownloadWalletResult

    /** The plan is still inside its buffer period. */
    data class BufferPeriod(val countdown: BufferPeriodCountdown) : ClaimDownloadWalletResult

    /**
     * No condition is complete yet. [validKeys] are the submitted accounts the plan accepted and
     * must be sent again on the next try; [keyOrigins] are further accounts to import from devices
     * already accepted. Both empty means another key (or a corrected account 0) is needed.
     */
    data class MoreKeysNeeded(
        val validKeys: List<ClaimValidKey>,
        val keyOrigins: List<KeyOrigin>,
    ) : ClaimDownloadWalletResult
}

/** One accepted account; compared as the full XFP/path/xpub tuple. */
data class ClaimValidKey(
    val xfp: String,
    val derivationPath: String,
    val xpub: String,
)
