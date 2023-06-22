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

package com.nunchuk.android.contact.components.pending.receive

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.databinding.ItemReceivedBinding
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.model.ReceiveContact
import com.nunchuk.android.widget.util.inflate

internal class ReceivedAdapter(
    private val accept: (ReceiveContact) -> Unit,
    private val cancel: (ReceiveContact) -> Unit
) : RecyclerView.Adapter<ContactViewHolder>() {

    internal var items: List<ReceiveContact> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ContactViewHolder(
        parent.inflate(R.layout.item_received),
        accept, cancel
    )

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

internal class ContactViewHolder(
    itemView: View,
    val accept: (ReceiveContact) -> Unit,
    val cancel: (ReceiveContact) -> Unit
) : BaseViewHolder<ReceiveContact>(itemView) {

    private val binding = ItemReceivedBinding.bind(itemView)

    override fun bind(data: ReceiveContact) {
        val contact = data.contact
        binding.avatar.text = contact.name.shorten()
        binding.name.text = contact.name
        binding.email.text = contact.email
        binding.email.isVisible = contact.isLoginInPrimaryKey().not()
        binding.accept.setOnClickListener { accept(data) }
        binding.cancel.setOnClickListener { cancel(data) }
    }

}