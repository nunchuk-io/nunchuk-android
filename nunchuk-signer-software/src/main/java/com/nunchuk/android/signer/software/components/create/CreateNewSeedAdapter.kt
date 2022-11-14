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

package com.nunchuk.android.signer.software.components.create

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ItemNewSeedBinding
import com.nunchuk.android.widget.util.inflate

internal class CreateNewSeedAdapter : RecyclerView.Adapter<CreateNewSeedViewHolder>() {

    internal var items: List<String> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CreateNewSeedViewHolder(
        parent.inflate(R.layout.item_new_seed)
    )

    override fun onBindViewHolder(holder: CreateNewSeedViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

internal class CreateNewSeedViewHolder(
    itemView: View
) : BaseViewHolder<String>(itemView) {
    private val binding = ItemNewSeedBinding.bind(itemView)

    override fun bind(data: String) {
        binding.seed.text = data
    }
}
