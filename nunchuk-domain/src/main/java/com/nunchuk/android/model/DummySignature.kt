package com.nunchuk.android.model

data class DummySignature(
    val xfp: String,
    val signature: String,
    val signedByUserId: String,
    val createdTimeMillis: Long
)