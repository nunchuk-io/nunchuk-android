package com.nunchuk.android.model.wallet

import com.nunchuk.android.model.signer.SignerServer

class ReplaceWalletStatus(
    val pendingReplaceXfps: List<String>,
    val signers: Map<String, SignerServer>
)