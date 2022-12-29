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
    val keyName: String? = null,
    @SerializedName("key_xfp")
    val keyXfp: String? = null,
    @SerializedName("card_id")
    val cardId: String? = null,
    @SerializedName("verification_type")
    val verificationType: String? = null,
    @SerializedName("verified_time_milis")
    val verifiedTimeMilis: Long? = null,
)