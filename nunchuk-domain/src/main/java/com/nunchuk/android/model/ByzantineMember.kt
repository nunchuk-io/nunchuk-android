package com.nunchuk.android.model

data class ByzantineMember(
    val emailOrUsername: String,
    val membershipId: String,
    val permissions: List<String>,
    val role: String,
    val status: String,
    val inviterUserId: String,
    val user: User?
) {
    fun isContact() = status == "ACTIVE"
    fun isPendingRequest() = status == "PENDING"
}