package com.nunchuk.android.network

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Data<out T>(
    @SerializedName("data")
    val data: T
) : Serializable
