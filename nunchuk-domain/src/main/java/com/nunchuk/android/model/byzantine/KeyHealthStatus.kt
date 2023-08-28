package com.nunchuk.android.model.byzantine

class KeyHealthStatus(
    val xfp: String,
    val canRequestHealthCheck: Boolean,
    val lastHealthCheckTimeMillis: Long
)