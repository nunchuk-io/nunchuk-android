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

package com.nunchuk.android.signer.software.components.confirm

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ItemConfirmSeedBinding
import com.nunchuk.android.widget.util.inflate

internal class ConfirmSeedAdapter(
    private val onItemUpdatedListener: (PhraseWordGroup) -> Unit
) : ListAdapter<PhraseWordGroup, ConfirmSeedViewHolder>(ITEM_DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ConfirmSeedViewHolder(
        parent.inflate(R.layout.item_confirm_seed),
        onItemUpdatedListener
    )

    override fun onBindViewHolder(holder: ConfirmSeedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val ITEM_DIFF = object: DiffUtil.ItemCallback<PhraseWordGroup>() {
            override fun areItemsTheSame(oldItem: PhraseWordGroup, newItem: PhraseWordGroup): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(oldItem: PhraseWordGroup, newItem: PhraseWordGroup): Boolean {
                return oldItem == newItem
            }
        }
    }
}

internal class ConfirmSeedViewHolder(
    itemView: View,
    val onItemUpdatedListener: (PhraseWordGroup) -> Unit
) : BaseViewHolder<PhraseWordGroup>(itemView) {

    private val binding = ItemConfirmSeedBinding.bind(itemView)

    override fun bind(data: PhraseWordGroup) {
        val wordNum = "Word #${data.index + 1}"
        binding.wordNum.text = wordNum
        binding.firstWord.text = data.firstWord.word
        binding.secondWord.text = data.secondWord.word
        binding.thirdWord.text = data.thirdWord.word

        bindState(binding.firstWord, data.firstWord.selected)
        bindState(binding.secondWord, data.secondWord.selected)
        bindState(binding.thirdWord, data.thirdWord.selected)

        binding.firstWord.setOnClickListener {
            onItemUpdatedListener(
                data.copy(
                    firstWord = data.firstWord.copy(selected = true),
                    secondWord = data.secondWord.copy(selected = false),
                    thirdWord = data.thirdWord.copy(selected = false)
                )
            )
        }
        binding.secondWord.setOnClickListener {
            onItemUpdatedListener(
                data.copy(
                    firstWord = data.firstWord.copy(selected = false),
                    secondWord = data.secondWord.copy(selected = true),
                    thirdWord = data.thirdWord.copy(selected = false)
                )
            )
        }
        binding.thirdWord.setOnClickListener {
            onItemUpdatedListener(
                data.copy(
                    firstWord = data.firstWord.copy(selected = false),
                    secondWord = data.secondWord.copy(selected = false),
                    thirdWord = data.thirdWord.copy(selected = true)
                )
            )
        }
    }

    private fun bindState(textView: TextView, selected: Boolean) {
        if (selected) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.nc_white_color))
            textView.background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_dark_thin_background)
        } else {
            textView.setTextColor(ContextCompat.getColor(context, R.color.nc_black_color))
            textView.background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_light_thin_background)
        }
    }

}
