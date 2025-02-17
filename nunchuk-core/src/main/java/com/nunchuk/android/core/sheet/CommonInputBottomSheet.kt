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

package com.nunchuk.android.core.sheet

import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.databinding.CommonInputBottomSheetBinding
import com.nunchuk.android.utils.parcelable
import kotlinx.parcelize.Parcelize

class CommonInputBottomSheet : BaseBottomSheet<CommonInputBottomSheetBinding>() {

    var listener: (String) -> Unit = {}

    private val args: Args? by lazy { arguments?.parcelable(ARGS) }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): CommonInputBottomSheetBinding {
        return CommonInputBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
    }

    private fun setupViews() {
        binding.edtName.setText(args?.defaultValue.orEmpty())
        binding.title.text = args?.title.orEmpty()

        val saveText = args?.action.orEmpty()
        val spannableString = SpannableString(saveText)
        spannableString.setSpan(
            UnderlineSpan(),
            0,
            saveText.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        binding.saveBtn.text = spannableString

        binding.closeBtn.setOnClickListener {
            cleanUp()
        }
        binding.saveBtn.setOnClickListener {
            save()
        }
    }

    private fun cleanUp() {
        binding.edtName.text?.clear()
        dismiss()
    }

    private fun save() {
        listener(binding.edtName.text.toString().trim())
        dismiss()
    }

    companion object {
        private const val TAG = "EditNameUserBottomSheet"
        const val ARGS = "Args"
        fun show(args: Args, fragmentManager: FragmentManager) = CommonInputBottomSheet().apply {
            arguments = bundleOf(
                ARGS to args
            )
            show(fragmentManager, TAG)
        }
    }

    @Parcelize
    data class Args(
        val title: String,
        val desc: String,
        val action: String,
        val defaultValue: String
    ) : Parcelable
}