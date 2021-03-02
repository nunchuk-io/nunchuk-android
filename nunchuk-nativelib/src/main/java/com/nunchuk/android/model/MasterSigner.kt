package com.nunchuk.android.model

data class MasterSigner(
        val id: String,
        val name: String,
        val device: Device,
        val lastHealthCheck: Long
)