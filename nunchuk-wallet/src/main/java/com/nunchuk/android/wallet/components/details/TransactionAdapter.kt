package com.nunchuk.android.wallet.components.details

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ItemTransactionBinding
import com.nunchuk.android.widget.util.inflate

internal class TransactionAdapter(
    private val listener: (Transaction) -> Unit
) : PagingDataAdapter<Transaction, TransactionViewHolder>(TransactionDiffCallback) {

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
) : BaseViewHolder<Transaction>(itemView) {

    private val binding = ItemTransactionBinding.bind(itemView)

    override fun bind(data: Transaction) {
        if (data.isReceive) {
            binding.sendTo.text = context.getString(R.string.nc_transaction_receive_at)
            binding.amountBTC.text = data.totalAmount.getBTCAmount()
            binding.amountUSD.text = data.totalAmount.getUSDAmount()
            binding.receiverName.text = data.receiveOutputs.firstOrNull()?.first.orEmpty().truncatedAddress()
        } else {
            if (data.status.isConfirmed()) {
                binding.sendTo.text = context.getString(R.string.nc_transaction_sent_to)
            } else {
                binding.sendTo.text = context.getString(R.string.nc_transaction_send_to)
            }
            binding.amountBTC.text = "- ${data.totalAmount.getBTCAmount()}"
            binding.amountUSD.text = "- ${data.totalAmount.getUSDAmount()}"
            binding.receiverName.text = data.outputs.firstOrNull()?.first.orEmpty().truncatedAddress()
        }
        binding.status.bindTransactionStatus(data)
        binding.date.text = data.getFormatDate()

        binding.root.setOnClickListener { onItemSelectedListener(data) }
    }

}

internal object TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {

    override fun areItemsTheSame(item1: Transaction, item2: Transaction) = item1.txId == item2.txId

    override fun areContentsTheSame(item1: Transaction, item2: Transaction) = item1.txId == item2.txId

}
