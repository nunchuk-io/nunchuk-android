package com.nunchuk.android.model

data class WalletServerSync(
    val isNeedReload: Boolean,
    val keyPolicyMap: Map<String, KeyPolicy>,
    val planWalletCreated: Set<String>
)