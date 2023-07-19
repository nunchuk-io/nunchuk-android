package com.nunchuk.android.main.membership.byzantine.groupdashboard

import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.membership.AssistedWalletBrief

sealed class GroupDashboardEvent {
    data class Loading(val loading: Boolean) : GroupDashboardEvent()
    data class Error(val message: String) : GroupDashboardEvent()
    object NavigateToGroupChat : GroupDashboardEvent()
}

data class GroupDashboardState(
    val walletExtended: WalletExtended = WalletExtended(),
    val members: List<ByzantineMember> = emptyList(),
    val group: ByzantineGroup? = null,
    val assistedWallets: List<AssistedWalletBrief> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val groupChat: GroupChat? = null
)