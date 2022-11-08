package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BackupKey(
    val keyId: String,
    val keyCheckSum: String,
    val keyBackUpBase64: String,
    val keyChecksumAlgorithm: String,
    val keyName: String
) : Parcelable