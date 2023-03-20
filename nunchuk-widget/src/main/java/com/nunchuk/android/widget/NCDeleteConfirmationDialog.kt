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

package com.nunchuk.android.widget

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import com.nunchuk.android.widget.databinding.NcDeleteConfirmDialogBinding
import javax.inject.Inject

class NCDeleteConfirmationDialog @Inject constructor(private val context: Context) {

    fun showDialog(
        title: String = context.getString(R.string.nc_confirmation),
        message: String = context.getString(R.string.nc_delete_account),
        onConfirmed: (String) -> Unit = {},
        onCanceled: () -> Unit = {},
        isMaskInput: Boolean = false,
    ) {
        Dialog(context).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            val binding = NcDeleteConfirmDialogBinding.inflate(LayoutInflater.from(context))
            setContentView(binding.root)
            if (isMaskInput) {
                binding.input.makeMaskedInput()
            }
            binding.title.text = title
            binding.message.text = message
            binding.btnYes.setOnClickListener {
                onConfirmed(binding.input.text.toString())
                dismiss()
            }

            binding.btnNo.setOnClickListener {
                onCanceled()
                dismiss()
            }
            show()
            window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        }
    }
}