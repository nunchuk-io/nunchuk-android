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

package com.nunchuk.android.signer.software.components.recover

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ItemRecoverSeedSuggestionBinding
import com.nunchuk.android.widget.util.inflate

internal class RecoverSeedSuggestionAdapter(
    private val onItemSelectedListener: (String) -> Unit
) : RecyclerView.Adapter<ConfirmSeedViewHolder>() {

    internal var items: List<String> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ConfirmSeedViewHolder(
        parent.inflate(R.layout.item_recover_seed_suggestion),
        onItemSelectedListener
    )

    override fun onBindViewHolder(holder: ConfirmSeedViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

internal class ConfirmSeedViewHolder(
    itemView: View,
    val onItemSelectedListener: (String) -> Unit
) : BaseViewHolder<String>(itemView) {

    private val binding = ItemRecoverSeedSuggestionBinding.bind(itemView)

    override fun bind(data: String) {
        binding.word.text = data
        binding.root.setOnClickListener { onItemSelectedListener(data) }
    }

}
