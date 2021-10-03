package com.nunchuk.android.messages.components.detail

import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.core.util.*
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ViewWalletStickyBinding
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.toRoomWalletData

fun ViewWalletStickyBinding.bindRoomWallet(wallet: RoomWallet, onClick: () -> Unit) {
    root.isVisible = wallet.isInitialized() && !wallet.isCanceled() && !wallet.isCreated()

    val roomWalletData = wallet.jsonContent.toRoomWalletData()
    name.text = roomWalletData.name
    configuration.bindRatio(isEscrow = roomWalletData.isEscrow, requireSigners = roomWalletData.requireSigners, totalSigners = roomWalletData.totalSigners)
    status.bindWalletStatus(roomWallet = wallet)
    root.setOnClickListener { onClick() }
}

fun ItemWalletBinding.bindRoomWallet(wallet: Wallet) {
    walletName.text = wallet.name
    config.bindRatio(isEscrow = wallet.escrow, requireSigners = wallet.totalRequireSigns, totalSigners = wallet.signers.size)
    val balanceVal = "(${wallet.getUSDAmount()})"
    btc.text = wallet.getBTCAmount()
    balance.text = balanceVal
}

private fun TextView.bindRatio(isEscrow: Boolean, requireSigners: Int, totalSigners: Int) {
    val walletType = if (isEscrow) {
        context.getString(R.string.nc_wallet_escrow_wallet)
    } else {
        context.getString(R.string.nc_wallet_standard_wallet)
    }
    val ratio = "$requireSigners / $totalSigners $walletType"
    text = ratio
}

fun TextView.bindWalletStatus(roomWallet: RoomWallet) {
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_fill_background)
    when {
        roomWallet.isPendingKeys() -> {
            context.getString(R.string.nc_message_pending_signers)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_red_tint_color)
        }
        roomWallet.isReadyFinalize() -> {
            context.getString(R.string.nc_message_pending_finalization)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_beeswax_tint)
        }
        roomWallet.isCanceled() -> {
            context.getString(R.string.nc_message_pending_finalization)
            background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_stroke_background)
        }
        else -> {
            context.getString(R.string.nc_text_created)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_green_color)
        }
    }
}