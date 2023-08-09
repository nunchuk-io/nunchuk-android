package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ByzantineMember(
    val emailOrUsername: String,
    val membershipId: String,
    val permissions: List<String>,
    val role: String,
    val status: String,
    val inviterUserId: String,
    val user: User?
) : Parcelable {
    fun isContact() = status == "ACTIVE"
    fun isPendingRequest() = status == "PENDING"
}