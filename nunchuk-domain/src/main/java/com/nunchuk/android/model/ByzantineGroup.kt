package com.nunchuk.android.model

import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.ByzantinePreferenceSetup

data class ByzantineGroup(
    val createdTimeMillis: Long,
    val id: String,
    val members: List<ByzantineMember>,
    val setupPreference: String,
    val status: String,
    val walletConfig: ByzantineWalletConfig,
    val isViewPendingWallet: Boolean,
) {
    fun isPendingWallet() = status == GroupStatus.PENDING_WALLET.name

    fun isSinglePersonSetup() = setupPreference == ByzantinePreferenceSetup.SINGLE_PERSON.name

    fun getMasterName() : String = members.find { it.role == AssistedWalletRole.MASTER.name }?.user?.name.orEmpty()
}