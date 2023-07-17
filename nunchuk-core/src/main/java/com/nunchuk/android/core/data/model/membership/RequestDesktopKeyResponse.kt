package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal data class DesktopKeyRequest(
    @SerializedName("tags")
    val tags: List<String>,
    @SerializedName("key_index")
    val keyIndex: Int? = null
)

internal data class RequestDesktopKeyResponse(
    @SerializedName("request")
    val request: Request? = null
)

internal data class Request(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("key")
    val key: SignerServerDto? = null,
    @SerializedName("status")
    val status: String = "",
)