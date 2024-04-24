package com.nunchuk.android.model.wallet

import com.nunchuk.android.model.byzantine.GroupWalletType
data class WalletOption(
    val slug: String,
    val recommended: Boolean,
    val name: String,
    val planName: String,
    val description: String,
    val badge: String,
    val walletType: GroupWalletType,
    val allowSoftKey: Boolean,
)