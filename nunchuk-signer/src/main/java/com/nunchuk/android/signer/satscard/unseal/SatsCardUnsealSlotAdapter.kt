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

package com.nunchuk.android.signer.satscard.unseal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ItemUnsealedSlotBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener

class SatsCardUnsealSlotAdapter(private val viewSlotAddress: (slot: SatsCardSlot) -> Unit) :
    ListAdapter<SatsCardSlot, SatsCardUnsealSlotHolder>(DIFF_ITEM) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatsCardUnsealSlotHolder {
        return SatsCardUnsealSlotHolder(ItemUnsealedSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)).apply {
            itemView.setOnDebounceClickListener {
                viewSlotAddress(getItem(bindingAdapterPosition))
            }
        }
    }

    override fun onBindViewHolder(holder: SatsCardUnsealSlotHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_ITEM = object : DiffUtil.ItemCallback<SatsCardSlot>() {
            override fun areItemsTheSame(oldItem: SatsCardSlot, newItem: SatsCardSlot): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(oldItem: SatsCardSlot, newItem: SatsCardSlot): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class SatsCardUnsealSlotHolder(private val binding: ItemUnsealedSlotBinding) : RecyclerView.ViewHolder(binding.root) {
    private val qrCodeSize = binding.root.context.resources.getDimensionPixelSize(R.dimen.nc_padding_36)


    fun bind(slot: SatsCardSlot) {
        binding.qrCode.setImageBitmap(slot.address.orEmpty().convertToQRCode(qrCodeSize, qrCodeSize))
        binding.tvAddress.text = slot.address
        binding.tvBalanceBtc.text = slot.balance.getBTCAmount()
    }
}