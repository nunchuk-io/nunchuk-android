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

package com.nunchuk.android.signer.satscard.wallets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.signer.databinding.ItemSelectWalletBinding

class SelectWalletAdapter(private val onItemSelect: (walletId: String) -> Unit) : ListAdapter<SelectableWallet, SelectWalletHolder>(ITEM_DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectWalletHolder {
        return SelectWalletHolder(
            onItemSelect,
            ItemSelectWalletBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SelectWalletHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val ITEM_DIFF_UTIL = object : DiffUtil.ItemCallback<SelectableWallet>() {
            override fun areItemsTheSame(oldItem: SelectableWallet, newItem: SelectableWallet): Boolean {
                return oldItem.wallet.id == newItem.wallet.id
            }

            override fun areContentsTheSame(oldItem: SelectableWallet, newItem: SelectableWallet): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class SelectWalletHolder(
    private val onItemSelect: (walletId: String) -> Unit,
    private val binding: ItemSelectWalletBinding
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.root.setOnClickListener {
            onItemSelect((binding.root.tag as SelectableWallet).wallet.id)
        }
    }

    fun bind(model: SelectableWallet) {
        binding.root.tag = model
        val wallet = model.wallet
        binding.radio.isChecked = model.isSelected
        binding.tvWalletName.text = wallet.name
        binding.tvBalance.text = wallet.getBTCAmount()
        binding.shareIcon.isVisible = model.isShared
        binding.config.bindWalletConfiguration(wallet)
    }
}