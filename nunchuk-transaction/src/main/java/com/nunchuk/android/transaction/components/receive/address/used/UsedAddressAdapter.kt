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

package com.nunchuk.android.transaction.components.receive.address.used

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.receive.address.UsedAddressModel
import com.nunchuk.android.transaction.databinding.ItemUsedAddressBinding
import com.nunchuk.android.widget.util.inflate

internal class UsedAddressAdapter(
    private val listener: (UsedAddressModel) -> Unit
) : RecyclerView.Adapter<UsedAddressViewHolder>() {

    internal var items: List<UsedAddressModel> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UsedAddressViewHolder(
        parent.inflate(R.layout.item_used_address),
        listener
    )

    override fun onBindViewHolder(holder: UsedAddressViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

internal class UsedAddressViewHolder(
    itemView: View,
    val listener: (UsedAddressModel) -> Unit
) : BaseViewHolder<UsedAddressModel>(itemView) {

    private val binding = ItemUsedAddressBinding.bind(itemView)

    override fun bind(data: UsedAddressModel) {
        binding.qrCode.setImageBitmap(data.address.convertToQRCode())
        binding.address.text = data.address
        binding.balance.text = data.balance.getBTCAmount()
        binding.root.setOnClickListener { listener(data) }
    }

}