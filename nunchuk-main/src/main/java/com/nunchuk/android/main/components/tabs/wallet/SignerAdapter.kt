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

package com.nunchuk.android.main.components.tabs.wallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.core.util.toReadableSignerTypeDrawable
import com.nunchuk.android.signer.databinding.ItemSignerBinding

class SignerAdapter(
    private val callback: (signer: SignerModel) -> Unit
) :
    ListAdapter<SignerModel, SingerViewHolder>(ITEM_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingerViewHolder {
        return SingerViewHolder(
            callback,
            ItemSignerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SingerViewHolder, position: Int) {
        holder.bindItem(getItem(position))
    }

    companion object {
        private val ITEM_CALLBACK = object : DiffUtil.ItemCallback<SignerModel>() {
            override fun areItemsTheSame(oldItem: SignerModel, newItem: SignerModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SignerModel, newItem: SignerModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class SingerViewHolder(
    private val callback: (signer: SignerModel) -> Unit,
    private val binding: ItemSignerBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindItem(model: SignerModel) {
        binding.signerType.apply {
            if (model.isPrimaryKey) {
                background = AppCompatResources.getDrawable(
                    binding.root.context,
                    R.drawable.nc_rounded_tag_primary_key_background
                )
                text = getString(R.string.nc_signer_type_primary_key)
            } else {
                background = AppCompatResources.getDrawable(
                    binding.root.context,
                    R.drawable.nc_rounded_tag_whisper_background
                )
                text = getString(R.string.nc_signer_type_software)
            }
        }
        binding.signerName.text = model.name
        binding.xpf.text = model.getXfpOrCardIdLabel()
        binding.signerType.text = model.toReadableSignerType(binding.root.context)
        binding.icPin.setImageDrawable(
            model.toReadableSignerTypeDrawable(
                binding.root.context,
                model.isPrimaryKey
            )
        )
        binding.root.setOnClickListener { callback(model) }
    }
}