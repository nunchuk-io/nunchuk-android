package com.nunchuk.android.model

data class Device(
    var type: String = "",
    var model: String = "",
    var path: String = "",
    var masterFingerprint: String = "",
    var connected: Boolean = false,
    var needPassPhraseSent: Boolean = false,
    var needPinSet: Boolean = false,
    var initialized: Boolean = true
)


