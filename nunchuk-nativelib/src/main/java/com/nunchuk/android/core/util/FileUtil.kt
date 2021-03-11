package com.nunchuk.android.core.util

import android.os.Environment
import java.io.File

object FileUtil {

    private const val NUNCHUK_DIR = "nunchuk"

    @Suppress("DEPRECATION")
    fun getOrCreateNunchukRootDir(): String {
        val mediaStorageDir = File(Environment.getExternalStorageDirectory(), NUNCHUK_DIR)
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
        }
        return mediaStorageDir.absolutePath
    }

}