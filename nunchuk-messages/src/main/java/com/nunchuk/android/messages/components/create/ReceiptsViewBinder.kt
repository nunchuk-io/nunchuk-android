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

package com.nunchuk.android.messages.components.create

import android.view.ViewGroup
import androidx.core.view.get
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.databinding.ItemReceiptBinding
import com.nunchuk.android.model.Contact
import com.nunchuk.android.widget.util.AbsViewBinder

class ReceiptsViewBinder(
    container: ViewGroup,
    receipts: List<Contact>,
    val callback: (Contact) -> Unit,
) : AbsViewBinder<Contact, ItemReceiptBinding>(container, receipts) {

    override fun initializeBinding() = ItemReceiptBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: Contact) {
        val binding = ItemReceiptBinding.bind(container.getChildAt(position))
        container[position].apply {
            binding.name.text = model.name
            binding.avatar.text = model.name.shorten()
            setOnClickListener { callback(model) }
        }
    }

}