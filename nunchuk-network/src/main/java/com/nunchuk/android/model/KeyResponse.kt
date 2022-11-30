package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

data class KeyResponse(
    @SerializedName("key_id")
    val keyId: String,
    @SerializedName("key_checksum")
    val keyCheckSum: String,
    @SerializedName("key_backup_base64")
    val keyBackUpBase64: String,
    @SerializedName("key_checksum_algorithm")
    val keyChecksumAlgorithm: String? = null,
    @SerializedName("key_name")
    val keyName: String? = null
)