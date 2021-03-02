package com.nunchuk.android.model

import com.nunchuk.android.type.BackendType
import com.nunchuk.android.type.Chain

data class AppSettings(
        val chain: Chain,
        val backendType: BackendType,
        val mainnetServers: List<String>,
        val testnetServers: List<String>,
        val hwiPath: String,
        val storagePath: String,
        val enableProxy: Boolean,
        val proxyHost: String,
        val proxyPort: Int,
        val proxyUsername: String,
        val proxyPassword: String,
        val certificateFile: String,
        val coreRpcHost: String,
        val coreRpcPort: Int,
        val coreRpcUsername: String,
        val coreRpcPassword: String,
)