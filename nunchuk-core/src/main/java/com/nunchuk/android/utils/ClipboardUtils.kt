package com.nunchuk.android.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import javax.inject.Inject

class TextUtils @Inject constructor(val context: Context) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyText(label: String = "Nunchuk", text: String) {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

}