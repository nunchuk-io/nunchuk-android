package com.nunchuk.android.model

import com.nunchuk.android.type.BackendType
import com.nunchuk.android.type.Chain

data class AppSettings(
        val chain: Chain = Chain.TESTNET,
        val backendType: BackendType = BackendType.ELECTRUM,
        val mainnetServers: List<String> = emptyList(),
        val testnetServers: List<String> = emptyList(),
        val hwiPath: String = "",
        val storagePath: String = "",
        val enableProxy: Boolean = false,
        val proxyHost: String = "",
        val proxyPort: Int = 0,
        val proxyUsername: String = "",
        val proxyPassword: String = "",
        val certificateFile: String = "",
        val coreRpcHost: String = "",
        val coreRpcPort: Int = 0,
        val coreRpcUsername: String = "",
        val coreRpcPassword: String = "",
)