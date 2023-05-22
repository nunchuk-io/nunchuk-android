package com.nunchuk.android.model

data class ByzantineGroup(
    val createdTimeMillis: Long,
    val id: String,
    val members: List<ByzantineMember>,
    val setupPreference: String,
    val status: String,
    val walletConfig: ByzantineWalletConfig
) {
    fun isPendingWallet() = status == "PENDING_WALLET"
}