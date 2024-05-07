package com.nunchuk.android.model

data class ElectrumServers(
    val mainnet: List<String>,
    val testnet: List<String>,
    val signet: List<String>
)