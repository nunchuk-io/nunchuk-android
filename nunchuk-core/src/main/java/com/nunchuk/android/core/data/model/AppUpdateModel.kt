package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AppUpdateResponse(
    @SerializedName("isUpdateAvailable")
    val isUpdateAvailable: Boolean? = null,
    @SerializedName("isUpdateRequired")
    val isUpdateRequired: Boolean? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("doItLaterCTALbl")
    val doItLaterCTALbl: String? = null
) : Serializable