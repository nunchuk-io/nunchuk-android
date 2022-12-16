package com.nunchuk.android.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class BackupKey(
    val keyId: String,
    val keyCheckSum: String,
    val keyBackUpBase64: String,
    val keyChecksumAlgorithm: String,
    val keyName: String,
    val keyXfp: String,
    val cardId: String,
    val verificationType: String,
    val verifiedTimeMilis: Long,
) : Parcelable