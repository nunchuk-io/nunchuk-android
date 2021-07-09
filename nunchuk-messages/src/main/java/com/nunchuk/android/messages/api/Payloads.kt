package com.nunchuk.android.messages.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AddContactPayload(
    @SerializedName("emails")
    val emails: List<String>
) : Serializable

data class AutoCompleteSearchContactPayload(
    @SerializedName("q")
    val keyword: String
) : Serializable