package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class CalculateRequiredSignaturesResponse(
    @SerializedName("result")
    val result: Data? = null
) {
    data class Data(
        @SerializedName("type")
        val type: String? = null,
        @SerializedName("required_signatures")
        val requiredSignatures: Int? = null
    )
}