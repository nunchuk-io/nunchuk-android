package com.nunchuk.android.main.membership.byzantine.groupdashboard

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.KeyHealthStatus

sealed class GroupDashboardEvent {
    data class Loading(val loading: Boolean) : GroupDashboardEvent()
    data class Error(val message: String) : GroupDashboardEvent()
    data class GetHistoryPeriodSuccess(val periods: List<HistoryPeriod>) : GroupDashboardEvent()
    class GetHealthCheckPayload(val payload: DummyTransactionPayload) : GroupDashboardEvent()
    data object RequestHealthCheckSuccess : GroupDashboardEvent()
    data class OpenEmergencyLockdown(val token: String = "") : GroupDashboardEvent()
    data class GetInheritanceSuccess(
        val inheritance: Inheritance,
        val token: String = "",
        val isAlertFlow: Boolean = false
    ) : GroupDashboardEvent()
    data class RegisterSignersSuccess(val totalAirgap: Int) : GroupDashboardEvent()
    data class UpdateServerKey(val token: String, val signer: SignerModel, val groupId: String) : GroupDashboardEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String
    ) : GroupDashboardEvent()

    data object RestartWizardSuccess: GroupDashboardEvent()
    data class SyncTransactionSuccess(val txId: String): GroupDashboardEvent()
    data class DownloadBackupKeySuccess(val backupKey: BackupKey) : GroupDashboardEvent()
    data object SignOutEvent : GroupDashboardEvent()
    data object OpenReplaceKey : GroupDashboardEvent()
}

data class GroupDashboardState(
    val groupId: String = "",
    val wallet: Wallet = Wallet(),
    val group: ByzantineGroup? = null,
    val alerts: List<Alert> = emptyList(),
    val groupChat: GroupChat? = null,
    val myRole: AssistedWalletRole = AssistedWalletRole.NONE,
    val signers: List<SignerModel> = emptyList(),
    val keyStatus: Map<String, KeyHealthStatus> = emptyMap(),
    val isAlreadySetupInheritance: Boolean = false,
    val inheritanceOwnerId: String? = null,
    val isHasPendingRequestInheritance: Boolean = false,
    val isRefreshing: Boolean = false,
    val isShowHealthCheckReminderIntro: Boolean = false,
    val walletStatus: String = "",
)