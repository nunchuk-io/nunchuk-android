package com.nunchuk.android.main.components.tabs.wallet.emptystate

import android.app.Activity
import androidx.core.content.ContextCompat
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator

class EmptyStateGroupMasterUser(
    private val navigator: NunchukNavigator,
    private val activityContext: Activity
) : EmptyStateProvider {
    override fun getWizardData(conditionInfo: ConditionInfo, isDark: Boolean): WizardData? {
        if (conditionInfo !is ConditionInfo.GroupMasterUser) return null
        return WizardData(
            title = activityContext.getString(R.string.nc_welcome_to_nunchuk),
            subtitle = activityContext.getString(R.string.nc_let_get_started_creating_first_wallet),
            instructions = emptyList(),
            buttonText = activityContext.getString(R.string.nc_wallet_create_wallet),
            buttonAction = {
                navigator.openWalletIntermediaryScreen(activityContext)
            },
            imageResId = R.drawable.bg_empty_state_group_plan,
            backgroundColor = ContextCompat.getColor(activityContext, R.color.nc_fill_denim)
        )
    }

    override fun getKeyWalletEntryData(conditionInfo: ConditionInfo): List<KeyWalletEntryData> {
        return emptyList()
    }
}