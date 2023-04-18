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

package com.nunchuk.android.core.sheet.input

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.databinding.DialogInputBottomSheetBinding
import com.nunchuk.android.core.util.setUnderline
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended

class InputBottomSheet : BaseBottomSheet<DialogInputBottomSheetBinding>() {

    private lateinit var listener: InputBottomSheetListener

    private val args: InputBottomSheetArgs by lazy { InputBottomSheetArgs.deserializeFrom(arguments) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): DialogInputBottomSheetBinding {
        return DialogInputBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (activity is InputBottomSheetListener) {
            activity as InputBottomSheetListener
        } else if (parentFragment is InputBottomSheetListener) {
            parentFragment as InputBottomSheetListener
        } else {
            throw IllegalArgumentException("activity or parentFragment must implement InputBottomSheetListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.title.text = args.title
        binding.edit.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_140))
        binding.edit.getEditTextView().setText(args.currentInput)
        binding.btnSave.setUnderline()

        binding.desc.isVisible = args.desc.isNullOrEmpty().not()
        binding.desc.text = args.desc

        binding.edit.addTextChangedCallback {
            binding.btnSave.isVisible = it.isNotEmpty()
        }

        binding.iconClose.setOnClickListener {
            onCloseClicked()
        }
        binding.btnSave.setOnClickListener {
            onSaveClicked()
        }
    }

    private fun onSaveClicked() {
        val newInput = binding.edit.getEditText()
        if (newInput != args.currentInput) {
            listener.onInputDone(newInput)
        }
        dismiss()
    }

    private fun onCloseClicked() {
        dismiss()
    }

    companion object {
        private const val TAG = "InputBottomSheet"

        private fun newInstance(currentInput: String, title: String, desc: String? = null) = InputBottomSheet().apply {
            arguments = InputBottomSheetArgs(title, desc, currentInput).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, currentInput: String, title: String, desc: String? = null): InputBottomSheet {
            return newInstance(currentInput, title, desc).apply { show(fragmentManager, TAG) }
        }
    }
}

interface InputBottomSheetListener {
    fun onInputDone(newInput: String)
}

data class InputBottomSheetArgs(val title: String, val desc: String? = null, val currentInput: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_CURRENT_INPUT, currentInput)
        putString(EXTRA_TITLE, title)
        putString(EXTRA_DESC, desc)
    }

    companion object {
        private const val EXTRA_CURRENT_INPUT = "EXTRA_CURRENT_INPUT"
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_DESC = "EXTRA_DESC"

        fun deserializeFrom(data: Bundle?) = InputBottomSheetArgs(
            data?.getString(EXTRA_TITLE).orEmpty(),
            data?.getString(EXTRA_DESC).orEmpty(),
            data?.getString(EXTRA_CURRENT_INPUT).orEmpty(),
        )
    }
}