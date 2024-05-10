package com.nunchuk.android.model

data class ElectrumServers(
    val mainnet: List<RemoteElectrumServer> = emptyList(),
    val testnet: List<RemoteElectrumServer> = emptyList(),
    val signet: List<RemoteElectrumServer> = emptyList()
)

data class RemoteElectrumServer(
    val name: String,
    val url: String
)