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

package com.nunchuk.android.contact.components.add

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.databinding.ItemEmailBinding
import com.nunchuk.android.widget.util.AbsViewBinder

class EmailsViewBinder(
    container: ViewGroup,
    emails: List<EmailWithState>,
    val callback: (EmailWithState) -> Unit,
) : AbsViewBinder<EmailWithState, ItemEmailBinding>(container, emails) {

    override fun initializeBinding() = ItemEmailBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: EmailWithState) {
        val binding = ItemEmailBinding.bind(container.getChildAt(position))
        binding.email.text = model.email
        if (model.valid) {
            binding.root.background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_green_background)
            binding.email.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_outline_24, 0, R.drawable.ic_close, 0)
        } else {
            binding.root.background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_red_background)
            binding.email.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_outline_24, 0, R.drawable.ic_close, 0)
        }
        binding.root.setOnClickListener { callback(model) }
    }

}