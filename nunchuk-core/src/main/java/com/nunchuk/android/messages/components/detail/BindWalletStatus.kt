package com.nunchuk.android.messages.components.detail

import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.isCanceled
import com.nunchuk.android.core.util.isCreated
import com.nunchuk.android.core.util.isPendingKeys
import com.nunchuk.android.core.util.isReadyFinalize
import com.nunchuk.android.model.RoomWallet

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
    text = context.getString(R.string.nc_text_created)
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
