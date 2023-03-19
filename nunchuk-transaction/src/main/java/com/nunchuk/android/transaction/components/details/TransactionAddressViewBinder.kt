package com.nunchuk.android.transaction.components.details

import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import com.nunchuk.android.compose.CoinTagGroupView
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.transaction.databinding.ItemTransactionAddressBinding
import com.nunchuk.android.widget.util.AbsViewBinder

class TransactionAddressViewBinder(
    container: ViewGroup,
    txOutputs: List<TxOutput>,
    private val tags: Map<Int, CoinTag>,
    private val tagIds: List<Set<Int>>,
    private val onCopyText: (text: String) -> Unit
) : AbsViewBinder<TxOutput, ItemTransactionAddressBinding>(container, txOutputs) {
    override fun initializeBinding(): ItemTransactionAddressBinding {
        return ItemTransactionAddressBinding.inflate(inflater, container, false)
    }

    override fun bindItem(position: Int, model: TxOutput) {
        val binding = ItemTransactionAddressBinding.bind(container[position])
        binding.sendAddressLabel.setOnLongClickListener {
            onCopyText(binding.sendAddressLabel.text.toString())
            true
        }
        binding.sendAddressLabel.text = model.first
        binding.sendAddressBTC.text = model.second.getBTCAmount()
        binding.sendAddressUSD.text = model.second.getUSDAmount()
        binding.tags.isVisible = tagIds[position].isNotEmpty()
        if (tagIds[position].isNotEmpty()) {
            binding.tags.setContent {
                CoinTagGroupView(tagIds = tagIds[position], tags = tags)
            }
        }
    }
}