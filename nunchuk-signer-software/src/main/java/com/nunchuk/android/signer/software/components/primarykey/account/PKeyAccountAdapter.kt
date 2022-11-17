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

package com.nunchuk.android.signer.software.components.primarykey.account

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ItemPkeyAccountBinding
import com.nunchuk.android.widget.util.inflate
import com.nunchuk.android.widget.util.setOnDebounceClickListener

internal class PrimaryKeyAccountAdapter(
    private val onItemClickListener: (PrimaryKey) -> Unit
) : ListAdapter<PrimaryKey, PrimaryKeyAccountViewHolder>(ITEM_DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PrimaryKeyAccountViewHolder(
        parent.inflate(R.layout.item_pkey_account),
        onItemClickListener
    )

    override fun onBindViewHolder(holder: PrimaryKeyAccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val ITEM_DIFF = object : DiffUtil.ItemCallback<PrimaryKey>() {
            override fun areItemsTheSame(oldItem: PrimaryKey, newItem: PrimaryKey): Boolean {
                return oldItem.masterFingerprint == newItem.masterFingerprint
            }

            override fun areContentsTheSame(oldItem: PrimaryKey, newItem: PrimaryKey): Boolean {
                return oldItem == newItem
            }
        }
    }
}

internal class PrimaryKeyAccountViewHolder(
    itemView: View,
    val onItemClickListener: (PrimaryKey) -> Unit
) : BaseViewHolder<PrimaryKey>(itemView) {

    private val binding = ItemPkeyAccountBinding.bind(itemView)

    override fun bind(data: PrimaryKey) {
        binding.textKeyName.text = data.name
        binding.textUserName.text = data.account
        binding.textXFP.text = String.format(getString(R.string.nc_primary_key_account_xfp), data.masterFingerprint)

        itemView.setOnDebounceClickListener {
            onItemClickListener(data)
        }
    }
}