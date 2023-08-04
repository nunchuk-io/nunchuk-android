package com.nunchuk.android.model

import com.nunchuk.android.model.byzantine.ByzantinePreferenceSetup

data class ByzantineGroup(
    val createdTimeMillis: Long,
    val id: String,
    val members: List<ByzantineMember>,
    val setupPreference: String,
    val status: String,
    val walletConfig: ByzantineWalletConfig
) {
    fun isPendingWallet() = status == GroupStatus.PENDING_WALLET.name

    fun isSinglePersonSetup() = setupPreference == ByzantinePreferenceSetup.SINGLE_PERSON.name
}