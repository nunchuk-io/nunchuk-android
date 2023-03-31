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

package com.nunchuk.android.core.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.R
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.type.TransactionStatus.*

fun TransactionStatus.canBroadCast() = this == READY_TO_BROADCAST

fun TransactionStatus.isPendingSignatures() = this == PENDING_SIGNATURES

fun TransactionStatus.hadBroadcast() = this == CONFIRMED || this == NETWORK_REJECTED || this == REPLACED || this == PENDING_CONFIRMATION

fun TransactionStatus.isPending() = this == PENDING_SIGNATURES || this == READY_TO_BROADCAST

fun TransactionStatus.isRejected() = this == NETWORK_REJECTED

fun TransactionStatus.isPendingConfirm() = this == PENDING_CONFIRMATION

fun TransactionStatus.isConfirmed() = this == CONFIRMED

fun Transaction.getPendingSignatures() = 0.coerceAtLeast(m - signers.count(Map.Entry<String, Boolean>::value))

fun Transaction.isPendingSignatures() = status == PENDING_SIGNATURES

fun List<Transaction>.sorted(): List<Transaction> {
    return sortedWith(comparatorTransaction)
}

private val comparatorTransaction = Comparator<Transaction> { t1, t2 ->
    when {
        t1.txId == t2.replacedByTxid && t1.status == CONFIRMED && t2.status == REPLACED -> -1
        t1.txId != t2.replacedByTxid && t1.status == CONFIRMED && t2.status == REPLACED -> 1
        t1.status == CONFIRMED && t2.status == CONFIRMED -> t2.blockTime.compareTo(t1.blockTime)
        t1.status.compareTo(t2.status) == 0 -> t2.blockTime.compareTo(t1.blockTime)
        else -> t1.status.compareTo(t2.status)
    }
}

fun String.truncatedAddress(): String = if (length < PREFIX_LENGTH + SUFFIX_LENGTH) {
    this
} else {
    "${take(PREFIX_LENGTH)}...${takeLast(SUFFIX_LENGTH)}"
}

fun Transaction.formatAddress(context: Context) : String {
    return if (isReceive) {
        val address = receiveOutputs.firstOrNull()?.first.orEmpty()
        "${context.getString(R.string.nc_transaction_receive_at)} ${address.truncatedAddress()}"
    } else {
        val address = outputs.firstOrNull()?.first.orEmpty()
        "${context.getString(R.string.nc_transaction_send_to)} ${address.truncatedAddress()}"
    }
}

fun Transaction.hasChangeIndex() = outputs.isNotEmpty() && changeIndex >= 0 && changeIndex < outputs.size

fun CoinStatus.toName(context: Context) : String {
    return when(this) {
        CoinStatus.INCOMING_PENDING_CONFIRMATION, CoinStatus.OUTGOING_PENDING_CONFIRMATION -> context.getString(R.string.nc_pending_conf)
        CoinStatus.OUTGOING_PENDING_SIGNATURES -> context.getString(R.string.nc_pending_sig)
        CoinStatus.OUTGOING_PENDING_BROADCAST -> context.getString(R.string.nc_pending_broadcast)
        else -> ""
    }
}

fun CoinStatus.toColor(context: Context) : Int {
    return when(this) {
        CoinStatus.INCOMING_PENDING_CONFIRMATION, CoinStatus.OUTGOING_PENDING_CONFIRMATION -> ContextCompat.getColor(context, R.color.nc_lavender_tint_color)
        CoinStatus.OUTGOING_PENDING_SIGNATURES -> ContextCompat.getColor(context, R.color.nc_red_tint_color)
        CoinStatus.OUTGOING_PENDING_BROADCAST -> ContextCompat.getColor(context, R.color.nc_beeswax_tint)
        else -> ContextCompat.getColor(context, R.color.nc_beeswax_tint)
    }
}

internal const val PREFIX_LENGTH = 5
internal const val SUFFIX_LENGTH = 4
