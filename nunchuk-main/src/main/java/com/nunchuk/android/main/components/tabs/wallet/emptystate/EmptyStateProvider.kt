package com.nunchuk.android.main.components.tabs.wallet.emptystate

import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType

interface EmptyStateProvider {
    fun getWizardData(conditionInfo: ConditionInfo): WizardData?
    fun getKeyWalletEntryData(conditionInfo: ConditionInfo): KeyWalletEntryData?
}

data class WizardData(
    val title: String,
    val subtitle: String,
    val instructions: List<String>,
    val buttonText: String,
    val buttonAction: () -> Unit,
    val imageResId: Int,
    val backgroundColor: Int
)

data class KeyWalletEntryData(
    val title: String,
    val buttonAction: () -> Unit,
    val iconResId: Int,
)

sealed class ConditionInfo {
    data object None : ConditionInfo()
    data class FreeGuestUser(val hasSigner: Boolean) : ConditionInfo()
    data class PersonalPlanUser(
        val resumeWizard: Boolean,
        val resumeWizardMinutes: Int,
        val walletType: GroupWalletType?,
        val hasSigner: Boolean,
        val groupStep: MembershipStage,
        val assistedWalletId: String?,
        val plan: MembershipPlan
    ) : ConditionInfo()

    data class MultipleSubscriptionsUser(val hasSigner: Boolean) : ConditionInfo()
    data class GroupMasterUser(val hasSigner: Boolean) : ConditionInfo()
}