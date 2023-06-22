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

package com.nunchuk.android.wallet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.databinding.DialogInputBottomSheetBinding
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.setUnderline
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.wallet.core.R
import com.nunchuk.android.widget.util.addTextChangedCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputBipPathBottomSheet : BaseBottomSheet<DialogInputBottomSheetBinding>() {

    private lateinit var listener: InputBipPathBottomSheetListener

    private val args: InputBipPathBottomSheetArgs by lazy {
        InputBipPathBottomSheetArgs.deserializeFrom(
            arguments
        )
    }

    private val viewModel by viewModels<InputBipPathViewModel>()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogInputBottomSheetBinding {
        return DialogInputBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (activity is InputBipPathBottomSheetListener) {
            activity as InputBipPathBottomSheetListener
        } else if (parentFragment is InputBipPathBottomSheetListener) {
            parentFragment as InputBipPathBottomSheetListener
        } else {
            throw IllegalArgumentException("activity or parentFragment must implement InputBottomSheetListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        flowObserver(viewModel.event) {
            when (it) {
                is InputBipPathEvent.Loading -> showOrHideLoading(it.isLoading)
                is InputBipPathEvent.OnVerifyPath -> if (it.isValid) {
                    listener.onInputDone(args.masterSignerId, binding.edit.getEditText())
                    dismiss()
                } else {
                    binding.edit.setError(getString(R.string.nc_invalid_path))
                }
            }
        }
    }

    private fun setupViews() {
        binding.title.text = getString(R.string.nc_bip32_path)
        binding.edit.getEditTextView().setText(args.currentInput)
        binding.btnSave.setUnderline()

        binding.desc.text = getString(R.string.nc_bip32_path_example)

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
            viewModel.checkBipPath(newInput)
        } else {
            dismiss()
        }
    }

    private fun onCloseClicked() {
        dismiss()
    }

    companion object {
        private const val TAG = "InputBottomSheet"

        private fun newInstance(
            masterSignerId: String,
            currentInput: String
        ) = InputBipPathBottomSheet().apply {
            arguments = InputBipPathBottomSheetArgs(masterSignerId, currentInput).buildBundle()
        }

        fun show(
            fragmentManager: FragmentManager,
            masterSignerId: String,
            currentInput: String
        ): InputBipPathBottomSheet {
            return newInstance(masterSignerId, currentInput).apply { show(fragmentManager, TAG) }
        }
    }
}

interface InputBipPathBottomSheetListener {
    fun onInputDone(masterSignerId: String, newInput: String)
}

data class InputBipPathBottomSheetArgs(
    val masterSignerId: String,
    val currentInput: String
) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_CURRENT_INPUT, currentInput)
        putString(EXTRA_MASTER_SIGNER_ID, masterSignerId)
    }

    companion object {
        private const val EXTRA_MASTER_SIGNER_ID = "EXTRA_MASTER_SIGNER_ID"
        private const val EXTRA_CURRENT_INPUT = "EXTRA_CURRENT_INPUT"

        fun deserializeFrom(data: Bundle?) = InputBipPathBottomSheetArgs(
            data?.getString(EXTRA_MASTER_SIGNER_ID).orEmpty(),
            data?.getString(EXTRA_CURRENT_INPUT).orEmpty(),
        )
    }
}