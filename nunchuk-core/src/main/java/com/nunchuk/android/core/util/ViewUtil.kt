package com.nunchuk.android.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.R
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.type.TransactionStatus.*
import javax.inject.Inject

class TextUtils @Inject constructor(val context: Context) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyText(label: String = "Nunchuk", text: String) {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

}

fun TextView.setUnderline(text: String = "${getText()}") {
    SpannableString(text).apply {
        setSpan(UnderlineSpan(), 0, text.length, 0)
        setText(this)
    }
}

fun TransactionStatus.toDisplayedText(context: Context) = when (this) {
    PENDING_SIGNATURES -> context.getString(R.string.nc_transaction_pending_signatures)
    READY_TO_BROADCAST -> context.getString(R.string.nc_transaction_ready_to_broadcast)
    NETWORK_REJECTED -> context.getString(R.string.nc_transaction_network_rejected)
    PENDING_CONFIRMATION -> context.getString(R.string.nc_transaction_pending_confirmation)
    REPLACED -> context.getString(R.string.nc_transaction_replaced)
    CONFIRMED -> context.getString(R.string.nc_transaction_confirmed)
}

fun Button.bindEnableState(enable: Boolean) {
    isEnabled = enable
    isClickable = enable
    if (enable) {
        background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_dark_background)
        setTextColor(ContextCompat.getColor(context, R.color.nc_white_color))
    } else {
        background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_whisper_disable_background)
        setTextColor(ContextCompat.getColor(context, R.color.nc_grey_dark_color))
    }
}