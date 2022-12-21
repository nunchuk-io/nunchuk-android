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
import android.view.WindowManager
import androidx.core.view.isVisible
import com.nunchuk.android.widget.databinding.NcVerticalInputDialogBinding
import javax.inject.Inject

class NCVerticalInputDialog @Inject constructor(
    private val context: Context
) {
    private val binding = NcVerticalInputDialogBinding.inflate(LayoutInflater.from(context))

    fun showDialog(
        title: String,
        descMessage: String? = null,
        isMaskedInput: Boolean = false,
        errorMessage: String? = null,
        inputType: Int = TEXT_TYPE,
        onPositiveClicked: (String) -> Unit = {},
        positiveText: String? = null,
        cancellable: Boolean = false,
        negativeText: String? = null,
        neutralText: String? = null,
        defaultInput: String? = null,
        onNegativeClicked: () -> Unit = {}
    ) = Dialog(context).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (cancellable) {
            binding.container.setOnClickListener {
                dismiss()
            }
        }
        setContentView(binding.root)
        binding.title.text = title
        binding.btnPositive.setOnClickListener {
            onPositiveClicked(binding.message.getEditText())
            dismiss()
        }

        binding.btnNegative.setOnClickListener {
            onNegativeClicked()
            dismiss()
        }
        binding.message.setInputType(inputType)
        if (positiveText.isNullOrEmpty().not()) {
            binding.btnPositive.text = positiveText
        }
        if (negativeText.isNullOrEmpty().not()) {
            binding.btnNegative.text = negativeText
        }
        if (neutralText.isNullOrEmpty().not()) {
            binding.btnNeutral.text = neutralText
        }
        if (isMaskedInput) {
            binding.message.makeMaskedInput()
        }
        binding.tvDesc.isVisible = descMessage.isNullOrEmpty().not()
        binding.tvDesc.text = descMessage
        if (!errorMessage.isNullOrEmpty()) {
            binding.message.setError(errorMessage)
        } else {
            binding.message.hideError()
        }
        binding.message.getEditTextView().setText(defaultInput)
        binding.message.getEditTextView().requestFocus()
        window?.apply {
            setLayout(MATCH_PARENT, MATCH_PARENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
        show()
    }
}