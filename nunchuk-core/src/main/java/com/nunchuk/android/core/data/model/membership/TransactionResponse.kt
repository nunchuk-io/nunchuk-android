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

package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.transaction.ServerTransaction

data class TransactionResponse(
    @SerializedName("transaction") val transaction: TransactionServerDto? = null
)

data class TransactionsResponse(
    @SerializedName("transactions") val transactions: List<TransactionServerDto> = emptyList()
)

data class TransactionServerDto(
    @SerializedName("wallet_id") val walletId: String? = null,
    @SerializedName("wallet_local_id") val walletLocalId: String? = null,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("psbt") val psbt: String? = null,
    @SerializedName("hex") val hex: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("reject_msg") val rejectMsg: String? = null,
    @SerializedName("created_time_milis") val createdTimeMilis: Long = 0,
    @SerializedName("sign_time_milis") val signedAtMilis: Long = 0,
    @SerializedName("last_modified_time_milis") val lastModifiedTimeMilis: Long = 0L,
    @SerializedName("broadcast_time_milis") val broadCastTimeMillis: Long = 0L,
    @SerializedName("spending_limit_reached") val spendingLimitReach: SpendingLimitReach? = null
)

data class SpendingLimitReach(
    @SerializedName("message")
    val message: String? = null
)

internal fun TransactionServerDto.toServerTransaction() = ServerTransaction(
    type = type.orEmpty(),
    broadcastTimeInMilis = broadCastTimeMillis,
    spendingLimitMessage = spendingLimitReach?.message.orEmpty(),
    signedInMilis = signedAtMilis
)