package com.nunchuk.android.contact.api

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.UserResponse

data class UsersResponseWrapper(
    @SerializedName("users")
    val users: List<UserResponse>
)

data class ContactResponseWrapper(
    @SerializedName("friends")
    val users: List<UserResponse>
)

data class UserResponseWrapper(
    @SerializedName("friend")
    val user: UserResponse
)

data class AddContactsResponse(
    @SerializedName("failed_emails")
    val failedEmails: List<String>?
)
