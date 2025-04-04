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

import android.app.Activity
import android.app.Dialog
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import androidx.core.view.isVisible
import com.nunchuk.android.widget.databinding.NcInfoDialogBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import javax.inject.Inject

class NCInfoDialog @Inject constructor(
    private val activity: Activity
) {

    fun init(
        title: String = activity.getString(R.string.nc_text_info),
        message: CharSequence,
        btnYes: String = activity.getString(R.string.nc_text_got_it),
        btnInfo: String = "",
        onYesClick: () -> Unit = {},
        onInfoClick: () -> Unit = {},
        cancelable: Boolean = false,
    ) = Dialog(activity).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(cancelable)
        setCanceledOnTouchOutside(cancelable)
        val binding = NcInfoDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.title.text = title
        binding.btnYes.text = btnYes
        binding.message.text = message
        binding.btnInfo.isVisible = btnInfo.isNotBlank()
        binding.btnInfo.text = btnInfo
        binding.btnYes.setOnClickListener {
            onYesClick()
            dismiss()
        }
        binding.btnInfo.setOnClickListener {
            onInfoClick()
            dismiss()
        }
        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

    fun showDialog(
        title: String = activity.getString(R.string.nc_text_info),
        message: CharSequence,
        btnYes: String = activity.getString(R.string.nc_text_got_it),
        btnInfo: String = "",
        onYesClick: () -> Unit = {},
        onInfoClick: () -> Unit = {},
        cancelable: Boolean = false,
        showTextButton: Boolean = false,
    ) = Dialog(activity).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(cancelable)
        setCanceledOnTouchOutside(cancelable)
        val binding = NcInfoDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.title.text = title
        binding.btnYes.text = btnYes

        try {
            val pattern = "\\[B](.*?)\\[/B]".toRegex()
            val matchResult = pattern.find(message)
            matchResult?.let {
                val boldText = it.groupValues[1]
                val start = message.indexOf(boldText) - "[B]".length
                val end = start + boldText.length
                val spannableString = SpannableString(message.toString().replace("[B]", "").replace("[/B]", ""))
                spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.message.text = spannableString
            } ?: run {
                binding.message.text = message
            }
        } catch (e: Exception) {
            binding.message.text = message
        }

        binding.btnInfo.isVisible = btnInfo.isNotBlank()
        binding.btnInfo.text = btnInfo
        binding.btnYes.setOnDebounceClickListener {
            onYesClick()
            dismiss()
        }
        binding.btnInfo.setOnDebounceClickListener {
            onInfoClick()
            dismiss()
        }
        if (showTextButton) {
            binding.btnInfo.setBackgroundDrawable(null)
        }
        show()
        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }
}