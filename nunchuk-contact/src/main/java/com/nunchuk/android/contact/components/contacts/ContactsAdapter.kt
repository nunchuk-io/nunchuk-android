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

package com.nunchuk.android.contact.components.contacts

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.databinding.ItemContactBinding
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.model.Contact
import com.nunchuk.android.widget.swipe.SwipeLayout
import com.nunchuk.android.widget.util.inflate

internal class ContactsAdapter(
    private val listener: (Contact) -> Unit,
    private val deleteContact: (Contact) -> Unit
) : RecyclerView.Adapter<ContactViewHolder>() {

    internal var items: List<Contact> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ContactViewHolder(
        parent.inflate(R.layout.item_contact),
        listener, deleteContact
    )

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

internal class ContactViewHolder(
    itemView: View,
    val listener: (Contact) -> Unit,
    val deleteContact: (Contact) -> Unit
) : BaseViewHolder<Contact>(itemView) {

    private val binding = ItemContactBinding.bind(itemView)

    override fun bind(data: Contact) {
        binding.swipeLayout.showMode = SwipeLayout.ShowMode.LayDown
        binding.swipeLayout.addDrag(SwipeLayout.DragEdge.Left, binding.actionLayout)
        binding.delete.setOnClickListener { deleteContact(data) }

        binding.avatar.text = data.name.shorten()
        binding.name.text = data.name
        binding.email.text = data.email
        binding.email.isVisible = data.isLoginInPrimaryKey().not()
        binding.root.setOnClickListener { listener(data) }
    }

}