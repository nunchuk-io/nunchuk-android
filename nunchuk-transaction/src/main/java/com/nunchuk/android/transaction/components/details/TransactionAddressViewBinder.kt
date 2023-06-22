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

package com.nunchuk.android.transaction.components.details

import android.view.ViewGroup
import androidx.core.view.get
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.transaction.databinding.ItemTransactionAddressBinding
import com.nunchuk.android.widget.util.AbsViewBinder

class TransactionAddressViewBinder(
    container: ViewGroup,
    txOutputs: List<TxOutput>,
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
        binding.sendAddressUSD.text = model.second.getCurrencyAmount()
    }
}