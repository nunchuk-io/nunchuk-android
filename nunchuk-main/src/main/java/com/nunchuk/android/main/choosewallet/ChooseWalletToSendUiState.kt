package com.nunchuk.android.main.choosewallet

import com.nunchuk.android.core.wallet.WalletUiModel
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.membership.AssistedWalletBrief

data class ChooseWalletToSendUiState(
    val walletUiModels: List<WalletUiModel> = emptyList(),
    val assistedWallets: Map<String, AssistedWalletBrief> = hashMapOf(),
    val joinedGroups: Map<String, ByzantineGroup> = HashMap(),
    val groupWallets: HashSet<String> = hashSetOf(),
    val wallets: List<WalletExtended> = emptyList(),
    val roles: Map<String, AssistedWalletRole> = emptyMap(),
    val hasNoWallets: Boolean = false,
) 