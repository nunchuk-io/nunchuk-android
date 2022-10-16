package com.nunchuk.android.signer.software.components.data.api

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.UserResponse
import java.io.Serializable

data class PKeySignUpPayload(
    @SerializedName("address")
    val address: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("signature")
    val signature: String
) : Serializable

data class PKeySignInPayload(
    @SerializedName("address")
    val address: String?,
    @SerializedName("username")
    val username: String,
    @SerializedName("signature")
    val signature: String
) : Serializable

data class PKeyNoncePayload(
    @SerializedName("address")
    val address: String?,
    @SerializedName("username")
    val username: String,
    @SerializedName("nonce")
    val nonce: String?
) : Serializable

data class PKeyChangeKeyPayload(
    @SerializedName("new_key")
    val newKey: String,
    @SerializedName("old_signed_message")
    val oldSignedMessage: String,
    @SerializedName("new_signed_message")
    val newSignedMessage: String
) : Serializable

data class PKeyDeleteKeyPayload(
    @SerializedName("signed_message")
    val signedMessage: String
) : Serializable

data class UserResponseWrapper(
    @SerializedName("user")
    val user: UserResponse
) : Serializable