package com.nunchuk.android.model

sealed class KeyUpload {
    data class Progress(val value: Int) : KeyUpload()
    data class Data(val filePath: String) : KeyUpload()
    data class KeyVerified(val message: String) : KeyUpload()
}