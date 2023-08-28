package com.nunchuk.android.model.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlertPayload(
    val masterName: String,
    val pendingKeysCount: Int,
    val dummyTransactionId: String,
    val xfps: List<String>,
    val claimKey: Boolean
): Parcelable