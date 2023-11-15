package com.nunchuk.android.model.byzantine

import com.nunchuk.android.model.ByzantineWalletConfig
import com.nunchuk.android.model.signer.SignerServer

data class DraftWallet(
    val config: ByzantineWalletConfig,
    val isMasterSecurityQuestionSet: Boolean,
    val signers: List<SignerServer>
)