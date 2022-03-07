package com.nunchuk.android.core.profile

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserResponseWrapper(
    @SerializedName("user")
    val user: UserProfileResponse
) : Serializable

data class UserProfileResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("gender")
    val gender: String? = null,
    @SerializedName("avatar")
    val avatar: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("chat_id")
    val chatId: String? = null
) : Serializable

data class UpdateUserProfilePayload(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
) : Serializable

data class DeleteConfirmationPayload(
    @SerializedName("confirmation_code")
    val confirmationCode: String
) : Serializable