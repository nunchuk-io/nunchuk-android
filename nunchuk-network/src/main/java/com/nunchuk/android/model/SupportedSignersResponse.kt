package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

data class SupportedSignersData(
    @SerializedName("supported_signers") val supportedSigners: List<SupportedSigner>
)

data class SupportedSigner(
    @SerializedName("signer_type") val signerType: String,
    @SerializedName("signer_tag") val signerTag: String
)