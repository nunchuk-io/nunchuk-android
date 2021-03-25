package com.nunchuk.android.core.util

import android.content.Context
import javax.inject.Inject

class FileHelper @Inject constructor(
    private val context: Context
) {

    fun getOrCreateNunchukRootDir(): String {
        return context.applicationContext.getDir(NUNCHUK_DIR, Context.MODE_PRIVATE).absolutePath
    }

    companion object {
        private const val NUNCHUK_DIR = "nunchuk"

        fun from(context: Context) = FileHelper(context)
    }

}