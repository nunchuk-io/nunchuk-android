package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName

internal class ElectrumServersDto(
    @SerializedName("mainnet")
    val mainnet: List<NodeDto> = emptyList(),
    @SerializedName("testnet")
    val testnet: List<NodeDto> = emptyList(),
    @SerializedName("signet")
    val signet: List<NodeDto> = emptyList()
)

internal data class NodeDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("url")
    val url: String
)