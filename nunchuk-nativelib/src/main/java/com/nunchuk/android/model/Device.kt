package com.nunchuk.android.model

data class Device(
        val fingerPrint: String,
        val type: String,
        val model: String,
        val masterFingerprint: String,
        val connected: Boolean,
        val needPassPhraseSent: Boolean,
        val needPinSet: Boolean,
        val initialized: Boolean = true
)


