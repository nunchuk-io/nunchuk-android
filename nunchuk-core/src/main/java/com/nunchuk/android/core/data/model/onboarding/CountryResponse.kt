package com.nunchuk.android.core.data.model.onboarding

import com.google.gson.annotations.SerializedName

data class CountryDataResponse(
    @SerializedName("countries") val countries: List<CountryResponse>
)

data class CountryResponse(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String
)