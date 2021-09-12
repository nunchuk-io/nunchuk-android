package com.nunchuk.android.messages.components.detail

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ViewWalletStickyBinding
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.RoomWalletData
import com.nunchuk.android.model.toRoomWalletData

fun ViewWalletStickyBinding.bindRoomWallet(wallet: RoomWallet) {
    val context = root.context
    val roomWalletData = wallet.jsonContent.toRoomWalletData()
    name.text = roomWalletData.name
    bindRatio(context, roomWalletData)
    val hasPendingSigners = roomWalletData.requireSigners > wallet.joinEventIds.size
    bindStatus(context, hasPendingSigners)
}

private fun ViewWalletStickyBinding.bindRatio(context: Context, roomWalletData: RoomWalletData) {
    val walletType = if (roomWalletData.isEscrow) {
        context.getString(R.string.nc_wallet_escrow_wallet)
    } else {
        context.getString(R.string.nc_wallet_standard_wallet)
    }
    val ratio = "${roomWalletData.requireSigners} / ${roomWalletData.totalSigners} $walletType"
    configuration.text = ratio
}

private fun ViewWalletStickyBinding.bindStatus(context: Context, hasPendingSigners: Boolean) {
    if (hasPendingSigners) {
        status.background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_red_background)
        status.text = context.getString(R.string.nc_message_pending_signers)
    } else {
        status.text = context.getString(R.string.nc_message_pending_finalization)
        status.background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_beeswax_tint_background)
    }
}