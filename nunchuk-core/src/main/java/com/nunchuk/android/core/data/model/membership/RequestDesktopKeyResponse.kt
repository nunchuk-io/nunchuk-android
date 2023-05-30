package com.nunchuk.android.core.data.model.membership

internal data class DesktopKeyRequest(
    val tags: List<String>,
)

internal data class RequestDesktopKeyResponse(
    val request: Request? = null
)

internal data class Request(
    val created_time_millis: Long = 0L,
    val id: String? = null,
    val key: SignerServerDto? = null,
    val key_index: Int = 0,
    val status: String = "",
    val tags: List<String> = emptyList(),
    val user_id: String = ""
)