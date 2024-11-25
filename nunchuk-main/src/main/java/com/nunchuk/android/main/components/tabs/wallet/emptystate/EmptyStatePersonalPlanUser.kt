package com.nunchuk.android.main.components.tabs.wallet.emptystate

import android.app.Activity
import androidx.core.content.ContextCompat
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.nav.NunchukNavigator

class EmptyStatePersonalPlanUser(
    private val navigator: NunchukNavigator,
    private val activityContext: Activity
) : EmptyStateProvider {
    override fun getWizardData(conditionInfo: ConditionInfo, isDark: Boolean): WizardData? {
        if (conditionInfo !is ConditionInfo.PersonalPlanUser) return null
        if (conditionInfo.resumeWizard) {
            return WizardData(
                title = activityContext.getString(R.string.nc_you_almost_done),
                subtitle = activityContext.getString(
                    R.string.nc_estimate_remain_time,
                    conditionInfo.resumeWizardMinutes
                ),
                instructions = emptyList(),
                buttonText = activityContext.getString(R.string.nc_continue_setting_your_wallet),
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
                backgroundColor = ContextCompat.getColor(activityContext, R.color.nc_fill_denim)
            )
        }
        return WizardData(
            title = activityContext.getString(R.string.nc_let_s_get_you_started),
            subtitle = activityContext.getString(R.string.nc_assisted_wallet_intro_desc),
            instructions = listOf(),
            buttonText = activityContext.getString(R.string.nc_create_assisted_wallet_subscription_required),
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
            imageResId = if (conditionInfo.plan == MembershipPlan.HONEY_BADGER_PLUS) R.drawable.bg_empty_state_group_plan else {
                if (isDark) R.drawable.bg_empty_state_personal_plan_dark else R.drawable.bg_empty_state_personal_plan
            },
            backgroundColor = ContextCompat.getColor(activityContext, R.color.nc_fill_denim)
        )
    }

    override fun getKeyWalletEntryData(conditionInfo: ConditionInfo): List<KeyWalletEntryData> {
        if (conditionInfo !is ConditionInfo.PersonalPlanUser) return emptyList()
        return listOf(
            KeyWalletEntryData(
                title = activityContext.getString(R.string.nc_create_unassisted_wallet),
                buttonAction = {
                    navigator.openWalletIntermediaryScreen(activityContext, conditionInfo.hasSigner)
                },
                iconResId = R.drawable.ic_wallet_empty_state
            )
        )
    }
}