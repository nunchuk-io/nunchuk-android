package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class GetCurrentServerTimeResponse(
    @SerializedName("utc_millis")
    val utcMillis: Long? = null
)