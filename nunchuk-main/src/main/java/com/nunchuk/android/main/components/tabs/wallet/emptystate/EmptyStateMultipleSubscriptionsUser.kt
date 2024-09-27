package com.nunchuk.android.main.components.tabs.wallet.emptystate

import android.app.Activity
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator

class EmptyStateMultipleSubscriptionsUser(
    private val navigator: NunchukNavigator,
    private val activityContext: Activity
) : EmptyStateFactory {
    override fun getWizardData(conditionInfo: ConditionInfo): WizardData? {
        if (conditionInfo !is ConditionInfo.MultipleSubscriptionsUser) return null
        return WizardData(
            title = "Welcome to Nunchuk!",
            subtitle = "Let's get started by creating your first wallet.",
            instructions = emptyList(),
            buttonText = "Create wallet",
            buttonAction = {
                navigator.openWalletIntermediaryScreen(activityContext, conditionInfo.hasSigner)
            },
            imageResId = R.drawable.bg_empty_state_group_plan,
            backgroundColor = 0xFFD0E2FF
        )
    }

    override fun getKeyWalletEntryData(conditionInfo: ConditionInfo): KeyWalletEntryData? {
        return null
    }
}