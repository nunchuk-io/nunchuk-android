package com.nunchuk.android.wallet.details

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getFormatDate
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.core.util.toDisplayedText
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ItemTransactionBinding
import com.nunchuk.android.widget.util.inflate

internal class TransactionAdapter(
    private val listener: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionViewHolder>() {

    internal var items: List<Transaction> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TransactionViewHolder(
        parent.inflate(R.layout.item_transaction),
        listener
    )

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

internal class TransactionViewHolder(
    itemView: View,
    val onItemSelectedListener: (Transaction) -> Unit
) : BaseViewHolder<Transaction>(itemView) {

    private val binding = ItemTransactionBinding.bind(itemView)

    override fun bind(data: Transaction) {
        if (data.isReceive) {
            binding.sendTo.text = context.getString(R.string.nc_transaction_receive_from)
            binding.amountBTC.text = data.subAmount.getBTCAmount()
            binding.amountUSD.text = data.subAmount.getUSDAmount()
            binding.receiverName.text = data.receiveOutput.first().first
        } else {
            binding.sendTo.text = context.getString(R.string.nc_transaction_send_to)
            binding.amountBTC.text = "- ${data.subAmount.getBTCAmount()}"
            binding.amountUSD.text = "- ${data.subAmount.getUSDAmount()}"
            binding.receiverName.text = data.outputs.first().first
        }
        binding.status.text = data.status.toDisplayedText(context)
        binding.date.text = data.getFormatDate()

        binding.root.setOnClickListener { onItemSelectedListener(data) }
    }

}
