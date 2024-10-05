package com.nunchuk.android.main.components.tabs.wallet.emptystate

import android.app.Activity
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.nav.NunchukNavigator

class EmptyStatePersonalPlanUser(
    private val navigator: NunchukNavigator,
    private val activityContext: Activity
) : EmptyStateFactory {
    override fun getWizardData(conditionInfo: ConditionInfo): WizardData? {
        if (conditionInfo !is ConditionInfo.PersonalPlanUser) return null
        if (conditionInfo.resumeWizard) {
            return WizardData(
                title = "You’re almost done!",
                subtitle = "Est. time remaining: ${conditionInfo.resumeWizardMinutes} minutes",
                instructions = emptyList(),
                buttonText = "Continue setting up your wallet",
                buttonAction = {
                    navigator.openMembershipActivity(
                        activityContext = activityContext,
                        groupStep = conditionInfo.groupStep,
                        walletId = conditionInfo.assistedWalletId,
                        isPersonalWallet = true,
                        walletType = conditionInfo.walletType
                    )
                },
                imageResId = R.drawable.bg_empty_state_personal_plan,
                backgroundColor = 0xFFD0E2FF
            )
        }
        return WizardData(
            title = "Let’s get you started",
            subtitle = "This wizard will walk you through creating an assisted wallet step by step.",
            instructions = listOf(),
            buttonText = "Create assisted wallet (subscription required)",
            buttonAction = {
                if (conditionInfo.plan == MembershipPlan.HONEY_BADGER_PLUS) {
                    navigator.openMembershipActivity(
                        activityContext = activityContext,
                        groupStep = MembershipStage.NONE,
                        isPersonalWallet = false
                    )
                } else {
                    navigator.openMembershipActivity(
                        activityContext = activityContext,
                        groupStep = conditionInfo.groupStep,
                        walletId = conditionInfo.assistedWalletId,
                        isPersonalWallet = true,
                        walletType = conditionInfo.walletType
                    )
                }
            },
            imageResId = if (conditionInfo.plan == MembershipPlan.HONEY_BADGER_PLUS) R.drawable.bg_empty_state_group_plan else R.drawable.bg_empty_state_personal_plan,
            backgroundColor = 0xFFD0E2FF
        )
    }

    override fun getKeyWalletEntryData(conditionInfo: ConditionInfo): KeyWalletEntryData? {
        if (conditionInfo !is ConditionInfo.PersonalPlanUser) return null
        return KeyWalletEntryData(
            title = "Create unassisted wallet",
            buttonAction = {
                navigator.openWalletIntermediaryScreen(activityContext, conditionInfo.hasSigner)
            },
            iconResId = R.drawable.ic_wallet_empty_state
        )
    }
}