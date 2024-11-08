package com.nunchuk.android.main.components.tabs.wallet.emptystate

import android.app.Activity
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator

class EmptyStateFreeGuestUser(
    private val navigator: NunchukNavigator,
    private val activityContext: Activity
) :
    EmptyStateProvider {
    override fun getWizardData(conditionInfo: ConditionInfo): WizardData? {
        if (conditionInfo !is ConditionInfo.FreeGuestUser) return null
        if (conditionInfo.hasSigner) {
            return WizardData(
                title = activityContext.getString(R.string.nc_welcome_to_nunchuk),
                subtitle = activityContext.getString(R.string.nc_continue_creating_first_wallet_or_add_another_key),
                instructions = emptyList(),
                buttonText = activityContext.getString(R.string.nc_text_create_new_wallet),
                buttonAction = {
                    navigator.openWalletIntermediaryScreen(activityContext, true)
                },
                imageResId = R.drawable.bg_empty_state_free_account,
                backgroundColor = 0xFFD0E2FF
            )
        }
        return WizardData( // hot wallet
            title = activityContext.getString(R.string.nc_welcome_to_nunchuk),
            subtitle = activityContext.getString(R.string.nc_to_get_started),
            instructions = listOf(
                activityContext.getString(R.string.nc_empty_state_free_user_step_1),
                activityContext.getString(R.string.nc_empty_state_free_user_step_2),
            ),
            buttonText = activityContext.getString(R.string.nc_add_key),
            buttonAction = {
                navigator.openSignerIntroScreen(activityContext)
            },
            imageResId = R.drawable.bg_empty_state_free_account,
            backgroundColor = 0xFFFDEBD2
        )
    }

    override fun getKeyWalletEntryData(conditionInfo: ConditionInfo): KeyWalletEntryData? {
        if (conditionInfo !is ConditionInfo.FreeGuestUser) return null
        if (conditionInfo.hasSigner) {
            return KeyWalletEntryData(
                title = activityContext.getString(R.string.nc_add_another_key),
                buttonAction = {
                    navigator.openSignerIntroScreen(activityContext)
                },
                iconResId = R.drawable.ic_signer_empty_state
            )
        }
        return KeyWalletEntryData(
            title = activityContext.getString(R.string.nc_create_hot_wallet),
            buttonAction = {
                navigator.openHotWalletScreen(null, activityContext, false)
            },
            iconResId = R.drawable.ic_hot_wallet_empty_state
        )
    }
}