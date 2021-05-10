package com.nunchuk.android.model

data class MasterSigner(
    var id: String = "",
    var name: String = "",
    var device: Device = Device(),
    var lastHealthCheck: Long = 0,
    var software: Boolean = false
)