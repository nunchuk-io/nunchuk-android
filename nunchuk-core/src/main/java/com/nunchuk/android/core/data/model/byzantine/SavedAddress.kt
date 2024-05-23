package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.SavedAddress

data class SavedAddressListResponse(
    @SerializedName("addresses")
    val addresses: List<SavedAddressResponse>? = null
)

data class SavedAddressResponse(
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("label")
    val label: String? = null
)

data class SavedAddressRequest(
    @SerializedName("address")
    val address: String,
    @SerializedName("label")
    val label: String
)

fun SavedAddressResponse.toSavedAddress() = SavedAddress(
    address = address ?: "",
    label = label ?: ""
)

