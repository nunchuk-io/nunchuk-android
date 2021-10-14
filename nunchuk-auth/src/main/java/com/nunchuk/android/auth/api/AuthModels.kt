package com.nunchuk.android.auth.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserTokenResponse(
    @SerializedName("tokenId")
    val tokenId: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("expireInSeconds")
    val expireInSeconds: Long = 0
) : Serializable

data class UserResponseWrapper(
    @SerializedName("user")
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
    val gender: String,
    @SerializedName("avatar")
    val avatar: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("chat_id")
    val chatId: String,
) : Serializable