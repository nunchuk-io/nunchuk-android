package com.nunchuk.android.model.byzantine

class HealthCheckRequest(
    val keyXfp: String? = null,
    val groupId: String? = null,
    val walletId: String? = null,
    val walletLocalId: String? = null,
    val canHealthCheck: Boolean = false,
    val canCancel: Boolean = false,
)