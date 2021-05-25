package com.nunchuk.android.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.TextView
import com.nunchuk.android.core.R
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.type.TransactionStatus.*
import java.text.SimpleDateFormat
import java.util.*
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

fun Long.formatDate(): String {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss aaa", Locale.US)
    val date = Date(this * 1000)
    return dateFormat.format(date)
}