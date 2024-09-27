package com.nunchuk.android.main.components.tabs.wallet.emptystate

class EmptyStateNone : EmptyStateFactory {
    override fun getWizardData(conditionInfo: ConditionInfo): WizardData? {
        return null
    }

    override fun getKeyWalletEntryData(conditionInfo: ConditionInfo): KeyWalletEntryData? {
        return null
    }

}