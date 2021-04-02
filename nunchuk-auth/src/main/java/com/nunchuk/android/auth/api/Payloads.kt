package com.nunchuk.android.auth.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RegisterPayload(
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String
) : Serializable

data class SignInPayload(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
) : Serializable

data class RecoverPasswordPayload(
    @SerializedName("email")
    val email: String,
    @SerializedName("forgotPasswordToken")
    val forgotPasswordToken: String,
    @SerializedName("newPassword")
    val newPassword: String
) : Serializable

data class ChangePasswordPayload(
    @SerializedName("oldPassword")
    val oldPassword: String,
    @SerializedName("newPassword")
    val newPassword: String
) : Serializable

data class ForgotPasswordPayload(
    @SerializedName("email")
    val oldPassword: String
) : Serializable

