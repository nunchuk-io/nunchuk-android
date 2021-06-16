package com.nunchuk.android.messages.api

import com.google.gson.annotations.SerializedName

data class AddContactPayload(
    @SerializedName("name")
    val emails: List<String>
)