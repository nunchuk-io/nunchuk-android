package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.byzantine.KeyHealthStatus

internal data class WalletHealthStatusResponse(
    @SerializedName("statuses") val statuses: List<KeyHealthStatusDto> = emptyList()
)

internal data class KeyHealthStatusDto(
    @SerializedName("xfp") val xfp: String = "",
    @SerializedName("is_pending_health_check") val isPendingHealthCheck: Boolean = false,
    @SerializedName("can_request_health_check") val canRequestHealthCheck: Boolean = false,
    @SerializedName("last_health_check_time_millis") val lastHealthCheckTimeMillis: Long? = null
)

internal fun KeyHealthStatusDto.toDomainModel(): KeyHealthStatus {
    return KeyHealthStatus(
        xfp = xfp,
        isPendingHealthCheck = isPendingHealthCheck,
        canRequestHealthCheck = canRequestHealthCheck,
        lastHealthCheckTimeMillis = lastHealthCheckTimeMillis ?: 0L
    )
}
