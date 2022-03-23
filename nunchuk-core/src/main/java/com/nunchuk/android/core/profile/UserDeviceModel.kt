package com.nunchuk.android.core.profile

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserDeviceWrapper(
    @SerializedName("devices")
    val devices: List<UserDeviceResponse>
) : Serializable

data class UserDeviceResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("lastTs")
    val lastTs: Long? = null
) : Serializable

data class DeleteDevicesPayload(
    @SerializedName("devices")
    val devices: List<String>
) : Serializable

data class CompromiseDevicesPayload(
    @SerializedName("devices")
    val devices: List<String>
) : Serializable
