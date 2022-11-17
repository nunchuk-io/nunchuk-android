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

package com.nunchuk.android.wallet.components.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.wallet.core.databinding.DialogUpdateWalletBottomSheetBinding
import com.nunchuk.android.widget.util.addTextChangedCallback

class WalletUpdateBottomSheet : BaseBottomSheet<DialogUpdateWalletBottomSheetBinding>() {

    private lateinit var listener: (String) -> Unit

    private val args: WalletUpdateBottomSheetArgs by lazy { WalletUpdateBottomSheetArgs.deserializeFrom(arguments) }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = DialogUpdateWalletBottomSheetBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        binding.editWalletName.text?.append(args.walletName)

        binding.editWalletName.addTextChangedCallback {
            binding.btnSave.isVisible = it.trim().isNotEmpty()
        }
        binding.iconClose.setOnClickListener {
            onCloseClicked()
        }
        binding.btnSave.setOnClickListener {
            onSaveClicked()
        }
    }

    private fun onSaveClicked() {
        val newWalletName = binding.editWalletName.text?.trim()?.toString().orEmpty()
        if (newWalletName != args.walletName) {
            listener(newWalletName)
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

        private fun newInstance(signerName: String) = WalletUpdateBottomSheet().apply {
            arguments = WalletUpdateBottomSheetArgs(signerName).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, walletName: String): WalletUpdateBottomSheet {
            return newInstance(walletName).apply { show(fragmentManager, TAG) }
        }
    }
}