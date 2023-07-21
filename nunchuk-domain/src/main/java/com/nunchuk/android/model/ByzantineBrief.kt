package com.nunchuk.android.model

data class ByzantineGroupBrief(
    val groupId: String,
    val status: String,
    val createdTimeMillis: Long,
    val members: List<ByzantineMemberBrief>
) {
    fun isPendingWallet() = status == "PENDING_WALLET"
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