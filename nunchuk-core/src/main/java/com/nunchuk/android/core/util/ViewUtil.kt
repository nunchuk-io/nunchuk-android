/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.nunchuk.android.core.R
import com.nunchuk.android.model.Transaction
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

fun TextView.bindTransactionStatus(transaction: Transaction) {
    val status = transaction.status
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
            val confirmations = "${transaction.height} ${context.getString(R.string.nc_transaction_confirmations)}"
            text = confirmations
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

fun ImageView.loadImage(
    imageUrl: String,
    cornerRadius: Float?,
    placeHolder: Drawable?,
    errorHolder: Drawable?,
    circleCrop: Boolean?
) {
    Glide.with(context).load(imageUrl).apply {
        placeHolder?.let { placeholder(placeHolder) }
        errorHolder?.let { error(errorHolder) }
        cornerRadius?.let { transform(CenterCrop(), RoundedCorners(it.toInt())) }
        if (circleCrop.orFalse()) {
            apply(RequestOptions.circleCropTransform())
        }
    }.into(this)
}

fun Boolean?.orFalse(): Boolean {
    return this ?: false
}

fun Int?.orDefault(default: Int): Int {
    return this ?: default
}

fun Context.showAlertDialog(
    title: String,
    message: String,
    positiveButtonText: String,
    negativeButtonText: String,
    positiveClick: () -> Unit,
    negativeClick: () -> Unit
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(
            positiveButtonText
        ) { _, _ ->
            positiveClick.invoke()
        }
        .setNegativeButton(
            negativeButtonText
        ) { _, _ ->
            negativeClick.invoke()
        }
        .create()
        .show()
}