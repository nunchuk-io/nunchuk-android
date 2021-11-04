package com.nunchuk.android.contact.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UsersResponseWrapper(
    @SerializedName("users")
    val users: List<UserResponse>
) : Serializable

data class ContactResponseWrapper(
    @SerializedName("friends")
    val users: List<UserResponse>
) : Serializable

data class UserResponseWrapper(
    @SerializedName("friend")
    val user: UserResponse
) : Serializable

data class UserResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("gender")
    val gender: String?,
    @SerializedName("avatar")
    val avatar: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("chat_id")
    val chatId: String,
) : Serializable

data class AddContactsResponse(
    @SerializedName("failed_emails")
    val failedEmails: List<String>?
) : Serializable

data class InviteFriendResponse(
    val result: String
) : Serializable