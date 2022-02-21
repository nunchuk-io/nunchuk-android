package com.nunchuk.android.messages.components.detail

import android.text.Html
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.core.util.*
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ViewWalletStickyBinding
import com.nunchuk.android.model.*

fun ViewWalletStickyBinding.bindRoomWallet(
    wallet: RoomWallet,
    transactions: List<Transaction>,
    onClick: () -> Unit,
    onClickViewTransactionDetail: (txId: String) -> Unit
) {
    root.isVisible = wallet.isInitialized() && !wallet.isCanceled() && !wallet.isCreated()
    root.setOnClickListener { onClick() }

    val transaction = transactions.getOrNull(0)
    if (transaction != null && transaction.isPendingSignatures()) {
        bindPendingSignature(transaction)
        root.isVisible = true
        root.setOnClickListener {
            onClickViewTransactionDetail(
                transaction.txId
            )
        }

        return
    }

    val roomWalletData = wallet.jsonContent.toRoomWalletData()
    name.text = roomWalletData.name
    configuration.bindRatio(isEscrow = roomWalletData.isEscrow, requireSigners = roomWalletData.requireSigners, totalSigners = roomWalletData.totalSigners)
    status.bindWalletStatus(roomWallet = wallet)
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

fun ViewWalletStickyBinding.bindPendingSignature(transaction: Transaction) {
    icon.setImageDrawable(ContextCompat.getDrawable(icon.context, R.drawable.ic_pending_signatures))
    status.bindPendingSignatures()
    name.text = transaction.totalAmount.getBTCAmount()
    configuration.text = Html.fromHtml(name.context.getString(R.string.nc_message_transaction_sending_to, transaction.outputs.first().first.formatToShortBTCAddress()))
}

fun String.formatToShortBTCAddress(): String {
    val firstPart = this.take(4)
    val middlePart = "..."
    val endPart = this.takeLast(4)
    return firstPart.plus(middlePart).plus(endPart)
}