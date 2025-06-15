package com.nunchuk.android.core.wallet

import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.membership.AssistedWalletBrief

data class WalletUiModel(
    val wallet: WalletExtended,
    val assistedWallet: AssistedWalletBrief?,
    val isAssistedWallet: Boolean,
    val group: ByzantineGroup?,
    val role: AssistedWalletRole,
    val walletStatus: String,
    val isGroupWallet: Boolean = false
)