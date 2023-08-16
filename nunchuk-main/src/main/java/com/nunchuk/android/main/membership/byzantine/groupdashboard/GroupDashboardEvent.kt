package com.nunchuk.android.main.membership.byzantine.groupdashboard

import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.membership.AssistedWalletBrief

sealed class GroupDashboardEvent {
    data class Loading(val loading: Boolean) : GroupDashboardEvent()
    data class Error(val message: String) : GroupDashboardEvent()
    data class GetHistoryPeriodSuccess(val periods: List<HistoryPeriod>) : GroupDashboardEvent()
    object NavigateToGroupChat : GroupDashboardEvent()
}

data class GroupDashboardState(
    val walletExtended: WalletExtended = WalletExtended(),
    val group: ByzantineGroup? = null,
    val assistedWallets: List<AssistedWalletBrief> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val groupChat: GroupChat? = null,
    val myRole: AssistedWalletRole = AssistedWalletRole.NONE,
    val keyStatus: Map<String, KeyHealthStatus> = emptyMap(),
)