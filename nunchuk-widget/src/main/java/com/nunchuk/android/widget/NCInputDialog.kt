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

package com.nunchuk.android.widget

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.view.WindowManager
import androidx.core.view.isVisible
import com.nunchuk.android.widget.databinding.NcConfirmDialogBinding
import javax.inject.Inject

class NCInputDialog @Inject constructor(private val context: Context) {

    fun showDialog(
        title: String,
        confirmText: String = context.getString(R.string.nc_text_confirm),
        inputBoxTitle: String = "",
        onConfirmed: (String) -> Unit = {},
        onCanceled: () -> Unit = {},
        isMaskedInput: Boolean = true,
        errorMessage: String? = null,
        descMessage: String? = null,
        inputType: Int = TEXT_TYPE,
        clickablePhrases: List<Pair<String, () -> Unit>> = emptyList()
    ) = Dialog(context).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        val binding = NcConfirmDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.title.text = title
        binding.btnYes.text = confirmText
        binding.btnYes.setOnClickListener {
            onConfirmed(binding.message.getEditText())
            dismiss()
        }

        binding.btnNo.setOnClickListener {
            onCanceled()
            dismiss()
        }
        binding.message.setInputType(inputType)
        if (isMaskedInput) {
            binding.message.makeMaskedInput()
        }
        binding.tvDesc.isVisible = descMessage.isNullOrEmpty().not()
        binding.tvInputBoxTitle.isVisible = clickablePhrases.isNotEmpty() && inputBoxTitle.isEmpty().not()
        if (!descMessage.isNullOrEmpty() && clickablePhrases.isNotEmpty()) {
            val spannable = android.text.SpannableString(descMessage)
            clickablePhrases.forEach { (phrase, callback) ->
                var start = descMessage.indexOf(phrase)
                while (start >= 0) {
                    val end = start + phrase.length
                    val boldSpan = android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
                    spannable.setSpan(boldSpan, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val clickableSpan = object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: android.view.View) {
                            callback()
                        }
                    }
                    spannable.setSpan(clickableSpan, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    start = descMessage.indexOf(phrase, end)
                }
            }
            binding.tvDesc.text = spannable
            binding.tvDesc.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            binding.tvDesc.highlightColor = android.graphics.Color.TRANSPARENT
        } else {
            binding.tvDesc.text = descMessage
        }
        if (!errorMessage.isNullOrEmpty()) {
            binding.message.setError(errorMessage)
        } else {
            binding.message.hideError()
        }
        binding.message.getEditTextView().requestFocus()
        window?.apply {
            setLayout(MATCH_PARENT, MATCH_PARENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
        show()
    }
}