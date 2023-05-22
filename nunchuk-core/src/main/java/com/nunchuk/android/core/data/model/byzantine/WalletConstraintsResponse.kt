package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

class WalletConstraintsDataResponse(
    @SerializedName("data")
    val data: WalletConstraintsResponse? = null
)

class WalletConstraintsResponse(
    @SerializedName("maximum_keyholder")
    val maximumKeyholder: Int? = null
)