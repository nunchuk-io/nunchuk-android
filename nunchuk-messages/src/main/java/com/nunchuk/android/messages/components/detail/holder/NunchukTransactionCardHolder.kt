/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.messages.components.detail.holder

import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.*
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukTransactionMessage
import com.nunchuk.android.messages.databinding.ItemTransactionCardBinding
import com.nunchuk.android.model.Transaction
import timber.log.Timber

internal class NunchukTransactionCardHolder(
    val binding: ItemTransactionCardBinding,
    val signTransaction: () -> Unit = {},
    val viewTransaction: (walletId: String, txId: String, initEventId: String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: NunchukTransactionMessage) {
        Timber.tag(TAG).d("bind(transactions::${model.transaction}, model::$model)")
        val initEventId = model.timelineEvent.eventId
        Timber.tag(TAG).d("initEventId::$initEventId")
        model.transaction?.let {
            bindTransaction(walletId = model.walletId, initEventId = initEventId, transaction = model.transaction, numAddress = model.numSendingAddress)
        } ?: run {
            bindUnknownTransaction()
        }
        Timber.tag(TAG).d("bindTransaction(walletId = ${model.walletId}, initEventId = $initEventId, transaction = ${model.transaction})")
        CardHelper.adjustCardLayout(binding.root, binding.cardTopContainer, model.isOwner)
    }

    private fun bindTransaction(walletId: String, initEventId: String, transaction: Transaction, numAddress: Int) {
        val context = itemView.context
        binding.amount.text = transaction.totalAmount.getBTCAmount()
        binding.status.bindTransactionStatus(transaction)
        val resId = if (transaction.status.isConfirmed()) {
            R.string.nc_message_transaction_sent_to
        } else {
            R.string.nc_message_transaction_sending_to
        }
        if (numAddress >= 2) {
            binding.address.text = getHtmlString(resId, getString(R.string.nc_multiple_addresses))
        } else {
            binding.address.text = getHtmlString(resId, transaction.outputs.first().first)
        }
        val pendingSigners = transaction.getPendingSignatures()
        if (pendingSigners > 0) {
            binding.signatureStatus.text =
                context.resources.getQuantityString(R.plurals.nc_transaction_pending_signature, pendingSigners, pendingSigners)
        } else {
            binding.signatureStatus.text = context.getString(R.string.nc_message_transaction_enough_signature)
        }
        binding.signatureStatus.isInvisible = transaction.status.hadBroadcast()
        binding.sign.setOnClickListener { signTransaction() }
        binding.viewDetails.setOnClickListener { viewTransaction(walletId, transaction.txId, initEventId) }
    }

    private fun bindUnknownTransaction() {
        binding.amount.text = ""
        binding.status.text = ""
        binding.address.text = ""
        binding.signatureStatus.text = ""
        binding.signatureStatus.isInvisible = false
        binding.sign.setOnClickListener(null)
        binding.viewDetails.setOnClickListener(null)
    }

    companion object {
        private const val TAG = "NunchukTransactionCardHolder"
    }

}