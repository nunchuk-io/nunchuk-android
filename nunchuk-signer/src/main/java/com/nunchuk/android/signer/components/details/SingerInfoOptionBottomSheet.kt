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

package com.nunchuk.android.signer.components.details

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.signer.components.details.model.SingerOption
import com.nunchuk.android.signer.databinding.DialogSignerDetailOptionsSheetBinding
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.serializable

class SingerInfoOptionBottomSheet : BaseBottomSheet<DialogSignerDetailOptionsSheetBinding>(), View.OnClickListener {
    private lateinit var listener : OptionClickListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OptionClickListener) {
            context
        } else if (parentFragment is OptionClickListener) {
            parentFragment as OptionClickListener
        } else {
            throw NullPointerException("Activity or Parent fragment have to implement OptionClickListener")
        }
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogSignerDetailOptionsSheetBinding {
        return DialogSignerDetailOptionsSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        registerEvents()
    }

    private fun initViews() {
        val signerType = requireArguments().serializable<SignerType>(EXTRA_SIGNER_TYPE)!!
        binding.btnChangeCvc.isVisible = signerType == SignerType.NFC
        binding.btnTopUpXpu.isVisible = signerType == SignerType.NFC
        binding.btnBackUpKey.isVisible = signerType == SignerType.NFC
        binding.btnSignMessage.isVisible = signerType == SignerType.NFC || signerType == SignerType.SOFTWARE
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            binding.btnTopUpXpu.id -> listener.onOptionClickListener(SingerOption.TOP_UP)
            binding.btnBackUpKey.id -> listener.onOptionClickListener(SingerOption.BACKUP_KEY)
            binding.btnChangeCvc.id -> listener.onOptionClickListener(SingerOption.CHANGE_CVC)
            binding.btnRemoveKey.id -> listener.onOptionClickListener(SingerOption.REMOVE_KEY)
            binding.btnSignMessage.id -> listener.onOptionClickListener(SingerOption.SIGN_MESSAGE)
        }
        dismiss()
    }

    private fun registerEvents() {
        binding.btnBackUpKey.setOnClickListener(this)
        binding.btnTopUpXpu.setOnClickListener(this)
        binding.btnChangeCvc.setOnClickListener(this)
        binding.btnRemoveKey.setOnClickListener(this)
        binding.btnSignMessage.setOnClickListener(this)
    }

    interface OptionClickListener {
        fun onOptionClickListener(option: SingerOption)
    }

    companion object {
        private const val EXTRA_SIGNER_TYPE = "EXTRA_SIGNER_TYPE"

        fun newInstance(signerType: SignerType) = SingerInfoOptionBottomSheet().apply {
            arguments = Bundle().apply {
                putSerializable(EXTRA_SIGNER_TYPE, signerType)
            }
        }
    }
}