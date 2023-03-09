package com.nunchuk.android.settings.walletsecurity

import com.nunchuk.android.model.setting.WalletSecuritySetting

data class WalletSecuritySettingState(
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting(),
    val walletPin: String = ""
)

sealed class WalletSecuritySettingEvent {
    object UpdateConfigSuccess : WalletSecuritySettingEvent()
    data class CheckWalletPin(val match: Boolean) : WalletSecuritySettingEvent()
    data class Loading(val loading: Boolean) : WalletSecuritySettingEvent()
    data class Error(val message: String) : WalletSecuritySettingEvent()
    object None : WalletSecuritySettingEvent()
}