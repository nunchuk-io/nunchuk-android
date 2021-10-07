package com.nunchuk.android.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
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

fun TextView.setUnderline() {
    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
}

fun TextView.bindTransactionStatus(status: TransactionStatus) {
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_fill_background)
    when (status) {
        PENDING_SIGNATURES -> {
            text = context.getString(R.string.nc_transaction_pending_signatures)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_red_tint_color)
        }
        READY_TO_BROADCAST -> {
            text = context.getString(R.string.nc_transaction_ready_to_broadcast)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_beeswax_tint)
        }
        PENDING_CONFIRMATION -> {
            text = context.getString(R.string.nc_transaction_pending_confirmation)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_lavender_tint_color)
        }
        CONFIRMED -> {
            text = context.getString(R.string.nc_transaction_confirmed)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_denim_tint_color)
        }
        NETWORK_REJECTED -> {
            text = context.getString(R.string.nc_transaction_network_rejected)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_orange_dark_color)
        }
        REPLACED -> {
            text = context.getString(R.string.nc_transaction_replaced)
            background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_stroke_background)
        }
    }
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