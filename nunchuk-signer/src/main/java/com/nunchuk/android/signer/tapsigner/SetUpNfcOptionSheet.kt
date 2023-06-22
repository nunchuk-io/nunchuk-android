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

package com.nunchuk.android.signer.tapsigner

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.signer.databinding.DialogSetUpOptionsSheetBinding

class SetUpNfcOptionSheet : BaseBottomSheet<DialogSetUpOptionsSheetBinding>(), View.OnClickListener {
    private lateinit var listener: OptionClickListener

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
    ): DialogSetUpOptionsSheetBinding {
        return DialogSetUpOptionsSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnAddNewNfc.id -> listener.onOptionClickListener(SetUpNfcOption.ADD_NEW)
            binding.btnRecoverNfcKey.id -> listener.onOptionClickListener(SetUpNfcOption.RECOVER)
            binding.btnAddMk4.id -> listener.onOptionClickListener(SetUpNfcOption.Mk4)
        }
        dismiss()
    }

    private fun registerEvents() {
        binding.btnAddNewNfc.setOnClickListener(this)
        binding.btnRecoverNfcKey.setOnClickListener(this)
        binding.btnAddMk4.setOnClickListener(this)
    }

    enum class SetUpNfcOption { ADD_NEW, RECOVER, Mk4 }

    interface OptionClickListener {
        fun onOptionClickListener(option: SetUpNfcOption)
    }

    companion object {
        fun newInstance() = SetUpNfcOptionSheet()
    }
}