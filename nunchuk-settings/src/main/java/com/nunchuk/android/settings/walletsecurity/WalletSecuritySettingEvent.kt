package com.nunchuk.android.settings.walletsecurity

import com.nunchuk.android.model.setting.WalletSecuritySetting

data class WalletSecuritySettingState(
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting()
)

sealed class WalletSecuritySettingEvent {
    object UpdateConfigSuccess : WalletSecuritySettingEvent()
    data class Loading(val loading: Boolean) : WalletSecuritySettingEvent()
    data class Error(val message: String) : WalletSecuritySettingEvent()
}