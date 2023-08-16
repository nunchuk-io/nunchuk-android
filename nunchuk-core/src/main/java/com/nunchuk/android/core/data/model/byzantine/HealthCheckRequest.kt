package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.CreateUpdateInheritancePlanRequest

class HealthCheckRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: CreateUpdateInheritancePlanRequest.Body? = null,
)