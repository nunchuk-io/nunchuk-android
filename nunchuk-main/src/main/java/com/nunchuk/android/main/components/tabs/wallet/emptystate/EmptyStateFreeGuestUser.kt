package com.nunchuk.android.main.components.tabs.wallet.emptystate

import android.app.Activity
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator

class EmptyStateFreeGuestUser(
    private val navigator: NunchukNavigator,
    private val activityContext: Activity
) :
    EmptyStateFactory {
    override fun getWizardData(conditionInfo: ConditionInfo): WizardData? {
        if (conditionInfo !is ConditionInfo.FreeGuestUser) return null
        if (conditionInfo.hasSigner) {
            return WizardData(
                title = "Welcome to Nunchuk!",
                subtitle = "Continue creating your first wallet, or add another key.",
                instructions = emptyList(),
                buttonText = "Create new wallet",
                buttonAction = {
                    navigator.openWalletIntermediaryScreen(activityContext, true)
                },
                imageResId = R.drawable.bg_empty_state_free_account,
                backgroundColor = 0xFFD0E2FF
            )
        }
        return WizardData( // hot wallet
            title = "Welcome to Nunchuk!",
            subtitle = "To get started:",
            instructions = listOf(
                "Add a key (or multiple keys if using multisig), then create your wallet.",
                "Or quickly create a hot wallet for immediate use, then back it up later."
            ),
            buttonText = "Add Key",
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
                title = "Add another key",
                buttonAction = {
                    navigator.openSignerIntroScreen(activityContext)
                },
                iconResId = R.drawable.ic_signer_empty_state
            )
        }
        return KeyWalletEntryData(
            title = "Create hot wallet",
            buttonAction = {
                navigator.openHotWalletScreen(null, activityContext, false)
            },
            iconResId = R.drawable.ic_hot_wallet_empty_state
        )
    }
}