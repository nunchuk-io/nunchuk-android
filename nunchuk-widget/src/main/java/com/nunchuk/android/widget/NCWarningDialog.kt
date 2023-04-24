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

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import androidx.core.text.HtmlCompat
import com.nunchuk.android.widget.databinding.NcWarningDialogBinding
import javax.inject.Inject

class NCWarningDialog @Inject constructor(
    private val activity: Activity
) {

    fun showDialog(
        title: String = activity.getString(R.string.nc_text_warning),
        message: String,
        btnYes: String = activity.getString(R.string.nc_text_yes),
        btnNo: String = activity.getString(R.string.nc_text_no),
        onYesClick: () -> Unit = {},
        onNoClick: () -> Unit = {}
    ) = Dialog(activity).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        val binding = NcWarningDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.title.text = title
        binding.btnYes.text = btnYes
        binding.btnNo.text = btnNo
        binding.message.text = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding.btnYes.setOnClickListener {
            onYesClick()
            dismiss()
        }

        binding.btnNo.setOnClickListener {
            onNoClick()
            dismiss()
        }
        show()
        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

}