package com.nunchuk.android.app.util

import android.os.Environment
import java.io.File

object FileUtil {

    fun createNunchukRootDir() {
        val mediaStorageDir = File(Environment.getExternalStorageDirectory(), "nunchuk")
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
        }
    }

}