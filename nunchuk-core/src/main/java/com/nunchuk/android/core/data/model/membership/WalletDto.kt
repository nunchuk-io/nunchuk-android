package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal data class WalletDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("local_id") val localId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("bsms") val bsms: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("server_key") val serverKeyDto: ServerKeyDto? = null,
    @SerializedName("signers") val signerServerDtos: List<SignerServerDto> = emptyList()
)