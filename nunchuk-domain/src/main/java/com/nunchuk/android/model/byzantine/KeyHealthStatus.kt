package com.nunchuk.android.model.byzantine

class KeyHealthStatus(
    val xfp: String,
    val isPendingHealthCheck: Boolean,
    val canRequestHealthCheck: Boolean,
    val lastHealthCheckTimeMillis: Long
)