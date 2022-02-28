package com.nunchuk.android.messages.components.detail

import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Transaction

fun TextView.bindWalletStatus(roomWallet: RoomWallet) {
    when {
        roomWallet.isCreated() -> bindCreatedStatus()
        roomWallet.isCanceled() -> bindCanceledStatus()
        roomWallet.isPendingKeys() -> bindPendingKeysStatus()
        roomWallet.isReadyFinalize() -> bindReadyFinalizeStatus()
        else -> bindCreatedStatus()
    }
}

private fun TextView.bindCreatedStatus() {
    text = context.getString(R.string.nc_text_completed)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_fill_background)
    backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_green_color)
}

private fun TextView.bindReadyFinalizeStatus() {
    text = context.getString(R.string.nc_message_pending_finalization)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_fill_background)
    backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_beeswax_tint)
}

private fun TextView.bindPendingKeysStatus() {
    text = context.getString(R.string.nc_message_pending_signers)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_fill_background)
    backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_red_tint_color)
}

fun TextView.bindCanceledStatus() {
    text = context.getString(R.string.nc_text_canceled)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_stroke_background)
}

fun TextView.bindPendingSignatures() {
    text = context.getString(R.string.nc_transaction_pending_signatures)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_stroke_background)
}
