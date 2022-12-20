package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

class InheritanceClaimStatusResponse(
    @SerializedName("inheritance")
    val inheritance: InheritanceDto? = null,
    @SerializedName("balance")
    val balance: Double? = null
)