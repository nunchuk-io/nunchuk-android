package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

// FIXME domain model
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
    @SerializedName("login_type")
    val loginType: String,
    @SerializedName("username")
    val username: String
)