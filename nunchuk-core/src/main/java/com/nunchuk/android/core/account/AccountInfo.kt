package com.nunchuk.android.core.account

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.guestmode.SignInMode

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
    val staySignedIn: Boolean = false,
    @SerializedName("avatar_url")
    val avatarUrl: String? = "",
    @SerializedName("device_id")
    val deviceId: String? = "",
    @SerializedName("login_type")
    val loginType: Int = SignInMode.UNKNOWN.value,
    @SerializedName("username")
    val username: String = "",
    @SerializedName("primary_key_info")
    val primaryKeyInfo: PrimaryKeyInfo? = null
)

data class PrimaryKeyInfo(
    @SerializedName("xfp")
    val xfp: String = ""
)
