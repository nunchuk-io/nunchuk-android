package com.nunchuk.android.model.byzantine

import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.signer.SignerServer

data class DraftWallet(
    val config: WalletConfig,
    val isMasterSecurityQuestionSet: Boolean,
    val signers: List<SignerServer>,
)