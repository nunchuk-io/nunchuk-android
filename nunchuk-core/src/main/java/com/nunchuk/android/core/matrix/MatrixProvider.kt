package com.nunchuk.android.core.matrix

import android.net.Uri
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import javax.inject.Inject

internal class MatrixProvider @Inject constructor() {

    fun getServerConfig() = HomeServerConnectionConfig
        .Builder()
        .withHomeServerUri(Uri.parse(HOME_SERVER_URI))
        .build()
}

internal const val HOME_SERVER_URI = "https://matrix.nunchuk.io"
