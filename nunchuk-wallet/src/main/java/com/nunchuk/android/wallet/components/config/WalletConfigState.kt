package com.nunchuk.android.wallet.components.config

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.WalletExtended

data class WalletConfigState(
    val walletExtended: WalletExtended = WalletExtended(),
    val signers: List<SignerModel> = emptyList(),
    val isAssistedWallet: Boolean = false
)