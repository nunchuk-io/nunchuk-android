package com.nunchuk.android.main.components.tabs.wallet.emptystate

import android.app.Activity
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator

class EmptyStateGroupMasterUser(
    private val navigator: NunchukNavigator,
    private val activityContext: Activity
) : EmptyStateProvider {
    override fun getWizardData(conditionInfo: ConditionInfo): WizardData? {
        if (conditionInfo !is ConditionInfo.GroupMasterUser) return null
        return WizardData(
            title = activityContext.getString(R.string.nc_welcome_to_nunchuk),
            subtitle = activityContext.getString(R.string.nc_let_get_started_creating_first_wallet),
            instructions = emptyList(),
            buttonText = activityContext.getString(R.string.nc_wallet_create_wallet),
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