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

package com.nunchuk.android.messages.components.detail

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.messages.databinding.BottomSheetSelectMessageBinding

internal class EditPhotoUserBottomSheet : BaseBottomSheet<BottomSheetSelectMessageBinding>() {

    lateinit var listener: (SelectMessageOption) -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSelectMessageBinding {
        return BottomSheetSelectMessageBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onActionClicked(SelectMessageOption.Dismiss)
    }

    private fun setupViews() {
        binding.btnSelect.setOnClickListener { onActionClicked(SelectMessageOption.Select) }
        binding.btnCopy.setOnClickListener { onActionClicked(SelectMessageOption.Copy) }
    }

    private fun onActionClicked(option: SelectMessageOption) {
        listener(option)
        dismiss()
    }

    companion object {
        private const val TAG = "WalletUpdateBottomSheet"

        fun show(fragmentManager: FragmentManager): EditPhotoUserBottomSheet {
            return EditPhotoUserBottomSheet().apply { show(fragmentManager, TAG) }
        }
    }
}

sealed class SelectMessageOption {
    object Select : SelectMessageOption()
    object Copy : SelectMessageOption()
    object Dismiss : SelectMessageOption()
}


