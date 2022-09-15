package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal data class CreateWalletRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("bsms") val bsms: String,
    @SerializedName("signers") val signers: List<SignerServerDto>,
    @SerializedName("local_id") val localId: String,
    @SerializedName("server_key_id") val serverKeyId: String
)