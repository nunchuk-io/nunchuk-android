package com.nunchuk.android.core.account

import com.google.gson.annotations.SerializedName

data class AccountInfo(
    @SerializedName("email")
    val email: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("token")
    val token: String = "",
    @SerializedName("chatId")
    val chatId: String = "",
    @SerializedName("activated")
    val activated: Boolean = false,
    @SerializedName("staySignedIn")
    val staySignedIn: Boolean = false
)
