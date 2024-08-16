package com.nunchuk.android.auth.domain.model

import com.google.gson.annotations.SerializedName

data class EmailAvailability(
    @SerializedName("available")
    val available: Boolean,
    @SerializedName("has_subscription")
    val hasSubscription: Boolean,
    @SerializedName("activated")
    val activated: Boolean
)