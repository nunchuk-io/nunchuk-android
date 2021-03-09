package com.nunchuk.android.model

data class Device(
        val fingerPrint: String = "",
        val type: String = "",
        val model: String = "",
        val masterFingerprint: String = "",
        val connected: Boolean = false,
        val needPassPhraseSent: Boolean = false,
        val needPinSet: Boolean = false,
        val initialized: Boolean = true
)


