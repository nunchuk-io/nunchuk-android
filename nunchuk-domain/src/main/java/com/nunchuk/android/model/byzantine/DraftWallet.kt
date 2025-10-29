package com.nunchuk.android.model.byzantine

import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.WalletType

data class DraftWallet(
    val config: WalletConfig,
    val isMasterSecurityQuestionSet: Boolean,
    val signers: List<SignerServer>,
    val walletType: WalletType?,
    val timelock: Int = 0,
    val replaceWallet: ReplaceWallet? = null
)