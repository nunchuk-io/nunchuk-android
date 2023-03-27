/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

    val transaction = transactions.firstOrNull { it.status.isPending() }
    if (transaction != null) {
        bindPendingSignature(transaction)
        root.isVisible = true
        root.setOnClickListener { onClickViewTransactionDetail(transaction.txId) }
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
    val balanceVal = "(${wallet.getCurrencyAmount()})"
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
    icon.setImageDrawable(ContextCompat.getDrawable(icon.context, R.drawable.ic_pending_transaction))
    status.bindTransactionStatus(transaction)
    name.text = transaction.totalAmount.getBTCAmount()
    val resId = if (transaction.status.isConfirmed()) {
        R.string.nc_message_transaction_sent_to
    } else {
        R.string.nc_message_transaction_sending_to
    }
    configuration.text = Html.fromHtml(name.context.getString(resId, transaction.outputs.first().first.formatToShortBTCAddress()))
}

fun String.formatToShortBTCAddress(): String {
    val firstPart = this.take(4)
    val middlePart = "..."
    val endPart = this.takeLast(4)
    return firstPart.plus(middlePart).plus(endPart)
}