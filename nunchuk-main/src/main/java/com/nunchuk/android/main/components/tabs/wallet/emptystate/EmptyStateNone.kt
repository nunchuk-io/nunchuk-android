package com.nunchuk.android.main.components.tabs.wallet.emptystate

class EmptyStateNone : EmptyStateProvider {
    override fun getWizardData(conditionInfo: ConditionInfo, isDark: Boolean): WizardData? {
        return null
    }

    override fun getKeyWalletEntryData(conditionInfo: ConditionInfo): List<KeyWalletEntryData> {
        return emptyList()
    }

}