package com.nunchuk.android.contact.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AddContactPayload(
    @SerializedName("emails")
    val emails: List<String>
) : Serializable

data class CancelRequestPayload(
    @SerializedName("contact_id")
    val accountId: String
) : Serializable

data class AcceptRequestPayload(
    @SerializedName("contact_id")
    val accountId: String
) : Serializable

data class AutoCompleteSearchContactPayload(
    @SerializedName("q")
    val keyword: String
) : Serializable

data class UpdateContactPayload(
    @SerializedName("avatar_url")
    val avatar: String
) : Serializable