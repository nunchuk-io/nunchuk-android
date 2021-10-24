package com.nunchuk.android.core.api

import com.google.gson.annotations.SerializedName

data class MatrixUploadFileResponse(
    @SerializedName("content_uri")
    val contentUri: String? = null
)