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

package com.nunchuk.android.wallet.components.details

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.simpleWeekDayYearFormat
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ItemTransactionBinding
import com.nunchuk.android.widget.util.inflate
import java.util.*

internal class TransactionAdapter(
    private val listener: (Transaction) -> Unit
) : PagingDataAdapter<ExtendedTransaction, TransactionViewHolder>(TransactionDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TransactionViewHolder(
        parent.inflate(R.layout.item_transaction),
        listener
    )

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }

}

internal class TransactionViewHolder(
    itemView: View,
    val onItemSelectedListener: (Transaction) -> Unit
) : BaseViewHolder<ExtendedTransaction>(itemView) {

    private val binding = ItemTransactionBinding.bind(itemView)

    override fun bind(data: ExtendedTransaction) {
        if (data.transaction.isReceive) {
            binding.sendTo.text = context.getString(R.string.nc_transaction_receive_at)
            binding.amountBTC.text = data.transaction.totalAmount.getBTCAmount()
            binding.amountUSD.text = data.transaction.totalAmount.getUSDAmount()
            binding.receiverName.text =
                data.transaction.receiveOutputs.firstOrNull()?.first.orEmpty().truncatedAddress()
        } else {
            if (data.transaction.status.isConfirmed()) {
                binding.sendTo.text = context.getString(R.string.nc_transaction_sent_to)
            } else {
                binding.sendTo.text = context.getString(R.string.nc_transaction_send_to)
            }
            binding.amountBTC.text = "- ${data.transaction.totalAmount.getBTCAmount()}"
            binding.amountUSD.text = "- ${data.transaction.totalAmount.getUSDAmount()}"
            binding.receiverName.text =
                data.transaction.outputs.firstOrNull()?.first.orEmpty().truncatedAddress()
        }
        binding.status.bindTransactionStatus(data.transaction)
        binding.date.text = data.transaction.getFormatDate()

        binding.root.setOnClickListener { onItemSelectedListener(data.transaction) }
        handleServerTransaction(data.transaction, data.serverTransaction)
    }

    private fun handleServerTransaction(
        transaction: Transaction,
        serverTransaction: ServerTransaction?
    ) {
        if (serverTransaction != null && transaction.status.canBroadCast() && serverTransaction.type == ServerTransactionType.SCHEDULED) {
            binding.status.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_schedule,
                0,
                0,
                0
            )
            val broadcastTime = Date(serverTransaction.broadcastTimeInMilis)
            binding.status.text = context.getString(
                R.string.nc_broadcast_on,
                broadcastTime.simpleWeekDayYearFormat(),
                broadcastTime.formatByHour()
            )
        } else {
            binding.status.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }
}

internal object TransactionDiffCallback : DiffUtil.ItemCallback<ExtendedTransaction>() {

    override fun areItemsTheSame(item1: ExtendedTransaction, item2: ExtendedTransaction) =
        item1.transaction.txId == item2.transaction.txId

    override fun areContentsTheSame(item1: ExtendedTransaction, item2: ExtendedTransaction) =
        item1.transaction.status == item2.transaction.status

}
