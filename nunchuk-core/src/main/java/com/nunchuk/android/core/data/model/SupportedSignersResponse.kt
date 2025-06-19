package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName

internal data class SupportedSignersData(
    @SerializedName("supported_signers") val supportedSigners: List<SupportedSigner>
)

internal data class SupportedSigner(
    @SerializedName("signer_type") val signerType: String,
    @SerializedName("signer_tag") val signerTag: String,
    @SerializedName("wallet_type") val walletType: String? = null,
    @SerializedName("address_type") val addressType: String
)