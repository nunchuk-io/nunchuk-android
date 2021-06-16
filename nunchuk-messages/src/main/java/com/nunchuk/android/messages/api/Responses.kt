package com.nunchuk.android.messages.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

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