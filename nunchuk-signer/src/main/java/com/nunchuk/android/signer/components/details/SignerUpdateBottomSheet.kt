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

package com.nunchuk.android.signer.components.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.setUnderline
import com.nunchuk.android.signer.databinding.DialogUpdateSignerBottomSheetBinding
import com.nunchuk.android.widget.util.addTextChangedCallback

class SignerUpdateBottomSheet : BaseBottomSheet<DialogUpdateSignerBottomSheetBinding>() {

    private lateinit var listener: (String) -> Unit

    private val args: SignerUpdateBottomSheetArgs by lazy { SignerUpdateBottomSheetArgs.deserializeFrom(arguments) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): DialogUpdateSignerBottomSheetBinding {
        return DialogUpdateSignerBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        val editSignerName: AppCompatEditText = binding.editSignerName
        editSignerName.text?.append(args.signerName)
        binding.btnSave.setUnderline()

        editSignerName.addTextChangedCallback {
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
        val newSignerName = binding.editSignerName.text.toString()
        if (newSignerName != args.signerName) {
            listener(newSignerName)
        }
        dismiss()
    }

    private fun onCloseClicked() {
        dismiss()
    }

    fun setListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "UpdateSignerBottomSheet"

        private fun newInstance(signerName: String) = SignerUpdateBottomSheet().apply {
            arguments = SignerUpdateBottomSheetArgs(signerName).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, signerName: String): SignerUpdateBottomSheet {
            return newInstance(signerName).apply { show(fragmentManager, TAG) }
        }
    }
}

data class SignerUpdateBottomSheetArgs(val signerName: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_SIGNER_NAME, signerName)
    }

    companion object {
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"

        fun deserializeFrom(data: Bundle?) = SignerUpdateBottomSheetArgs(
            data?.getString(EXTRA_SIGNER_NAME).orEmpty()
        )
    }
}