package com.nunchuk.android.model.wallet

import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.signer.SignerServer

class ReplaceWalletStatus(
    val pendingReplaceXfps: List<String> = emptyList(),
    val signers: Map<String, SignerServer> = emptyMap(),
    val replacements: Map<String, List<SignerServer>> = emptyMap(),
    val timelock: WalletTimelock? = null
)