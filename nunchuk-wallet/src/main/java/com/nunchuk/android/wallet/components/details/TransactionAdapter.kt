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

import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.TransactionNoteView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.bindTransactionStatus
import com.nunchuk.android.core.util.canBroadCast
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getFormatDate
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.truncatedAddress
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.simpleWeekDayYearFormat
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ItemTransactionBinding
import com.nunchuk.android.widget.util.inflate
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import java.util.Date

internal class TransactionAdapter(
    private val listener: (Transaction) -> Unit
) : PagingDataAdapter<ExtendedTransaction, TransactionAdapter.TransactionViewHolder>(
    TransactionDiffCallback
) {

    private var hideWalletDetail: Boolean = false

    fun setHideWalletDetail(hideWalletDetail: Boolean) {
        this.hideWalletDetail = hideWalletDetail
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TransactionViewHolder(
        parent.inflate(R.layout.item_transaction),
        listener
    )

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }

    inner class TransactionViewHolder(
        itemView: View,
        val onItemSelectedListener: (Transaction) -> Unit
    ) : BaseViewHolder<ExtendedTransaction>(itemView) {

        private val binding = ItemTransactionBinding.bind(itemView)
        private val receivedAmountColor = ContextCompat.getColor(context, R.color.nc_slime_dark)
        private val sentAmountColor = ContextCompat.getColor(context, R.color.nc_primary_color)

        init {
            binding.noteContainer.setOnDebounceClickListener {
                runCatching {
                    val note = it.tag as String
                    val matcher = Patterns.WEB_URL.matcher(note)
                    if (matcher.find()) {
                        val link = note.substring(matcher.start(1), matcher.end())
                        context.openExternalLink(link)
                    }
                }
            }
        }

        override fun bind(data: ExtendedTransaction) {
            if (data.transaction.isReceive) {
                binding.sendTo.text = context.getString(R.string.nc_transaction_receive_at)
                binding.amountBTC.text =
                    Utils.maskValue(data.transaction.totalAmount.getBTCAmount(), hideWalletDetail)
                binding.amountBTC.setTextColor(receivedAmountColor)
                binding.amountUSD.text = Utils.maskValue(
                    data.transaction.totalAmount.getCurrencyAmount(),
                    hideWalletDetail
                )
                if (data.transaction.receiveOutputs.size > 1) {
                    binding.receiverName.text = getString(R.string.nc_multiple_addresses)
                } else {
                    binding.receiverName.text = Utils.maskValue(
                        data.transaction.receiveOutputs.firstOrNull()?.first.orEmpty()
                            .truncatedAddress(), hideWalletDetail
                    )
                }
            } else {
                binding.sendTo.text = context.getString(R.string.nc_transaction_send_to)
                binding.amountBTC.text = Utils.maskValue(
                    "- ${data.transaction.totalAmount.getBTCAmount()}",
                    hideWalletDetail
                )
                binding.amountBTC.setTextColor(sentAmountColor)
                binding.amountUSD.text = Utils.maskValue(
                    "- ${data.transaction.totalAmount.getCurrencyAmount()}",
                    hideWalletDetail
                )
                val output = if (data.transaction.changeIndex >= 0) data.transaction.outputs.size - 1 else data.transaction.outputs.size
                if (output > 1) {
                    binding.receiverName.text = getString(R.string.nc_multiple_addresses)
                } else {
                    binding.receiverName.text =
                        Utils.maskValue(
                            data.transaction.outputs.firstOrNull()?.first.orEmpty()
                                .truncatedAddress(),
                            hideWalletDetail
                        )
                }
            }
            binding.status.bindTransactionStatus(data.transaction)
            binding.date.text = data.transaction.getFormatDate()

            binding.root.setOnClickListener {
                if (hideWalletDetail.not()) onItemSelectedListener(data.transaction)
            }
            handleServerTransaction(data.transaction, data.serverTransaction)
            binding.noteContainer.tag = data.transaction.memo
            binding.noteContainer.isVisible = data.transaction.memo.isNotEmpty()
            binding.noteContainer.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                setContent {
                    NunchukTheme(isSetStatusBar = false) {
                        TransactionNoteView(
                            modifier = Modifier
                                .border(1.dp, NcColor.border, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            note = data.transaction.memo
                        )
                    }
                }
            }
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
                if (serverTransaction.broadcastTimeInMilis > 0L) {
                    val broadcastTime = Date(serverTransaction.broadcastTimeInMilis)
                    binding.status.text = context.getString(
                        R.string.nc_broadcast_on,
                        broadcastTime.simpleWeekDayYearFormat(),
                        broadcastTime.formatByHour()
                    )
                }
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
}

