package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal data class SignerServerDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("xfp") val xfp: String? = null,
    @SerializedName("derivation_path") val derivationPath: String? = null,
    @SerializedName("xpub") val xpub: String? = null,
    @SerializedName("pubkey") val pubkey: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("tapsigner") val tapsigner: TapSignerDto? = null
)