package com.nunchuk.android.model.setting

data class WalletSecuritySetting(
    val hideWalletDetail: Boolean = false,
    val protectWalletPassword: Boolean = false,
    val protectWalletPassphrase: Boolean = false,
    val protectWalletPin: Boolean = false
)