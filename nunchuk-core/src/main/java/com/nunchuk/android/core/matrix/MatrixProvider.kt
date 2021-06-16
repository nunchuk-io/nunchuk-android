package com.nunchuk.android.core.matrix

import android.content.Context
import android.net.Uri
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import javax.inject.Inject

internal class MatrixProvider @Inject constructor(val context: Context) {

    fun getMatrix() = Matrix.getInstance(context = context)

    fun getServerConfig() = HomeServerConnectionConfig
        .Builder()
        .withHomeServerUri(Uri.parse("http://matrix.nunchuk.io"))
        .build()
}
