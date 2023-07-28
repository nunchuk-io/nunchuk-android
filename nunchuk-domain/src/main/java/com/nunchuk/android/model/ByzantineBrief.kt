package com.nunchuk.android.model

import com.nunchuk.android.model.byzantine.AssistedWalletRole

data class ByzantineGroupBrief(
    val groupId: String,
    val status: String,
    val createdTimeMillis: Long,
    val members: List<ByzantineMemberBrief>,
    val isViewPendingWallet: Boolean,
    val walletConfig: ByzantineWalletConfig
) {
    fun isPendingWallet() = status == "PENDING_WALLET"

    fun getMasterName() : String = members.find { it.role == AssistedWalletRole.MASTER.name }?.name.orEmpty()
}

data class ByzantineMemberBrief(
    val emailOrUsername: String,
    val role: String,
    val status: String,
    val inviterUserId: String,
    val userId: String? = null,
    val avatar: String? = null,
    val name: String? = null,
    val email: String? = null
) {
    fun isPendingRequest() = status == "PENDING"
}