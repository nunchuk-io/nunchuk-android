package com.nunchuk.android.messages.components.detail

import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ViewWalletStickyBinding
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.RoomWalletData
import com.nunchuk.android.model.toRoomWalletData

fun ViewWalletStickyBinding.bindRoomWallet(wallet: RoomWallet) {
    val roomWalletData = wallet.jsonContent.toRoomWalletData()
    name.text = roomWalletData.name
    configuration.bindRatio(roomWalletData)
    val hasPendingSigners = roomWalletData.requireSigners > wallet.joinEventIds.size
    bindStatus(status, hasPendingSigners)
}

fun ItemWalletBinding.bindRoomWallet(wallet: RoomWallet) {
    val roomWalletData = wallet.jsonContent.toRoomWalletData()
    walletName.text = roomWalletData.name
    config.bindRatio(roomWalletData)
    val amount = Amount.ZER0
    val balanceVal = "(${amount.getUSDAmount()})"
    btc.text = amount.getBTCAmount()
    balance.text = balanceVal
}

private fun TextView.bindRatio(roomWalletData: RoomWalletData) {
    val walletType = if (roomWalletData.isEscrow) {
        context.getString(R.string.nc_wallet_escrow_wallet)
    } else {
        context.getString(R.string.nc_wallet_standard_wallet)
    }
    val ratio = "${roomWalletData.requireSigners} / ${roomWalletData.totalSigners} $walletType"
    text = ratio
}

private fun bindStatus(status: TextView, hasPendingSigners: Boolean) {
    if (hasPendingSigners) {
        status.background = AppCompatResources.getDrawable(status.context, R.drawable.nc_rounded_red_background)
        status.text = status.context.getString(R.string.nc_message_pending_signers)
    } else {
        status.text = status.context.getString(R.string.nc_message_pending_finalization)
        status.background = AppCompatResources.getDrawable(status.context, R.drawable.nc_rounded_beeswax_tint_background)
    }
}