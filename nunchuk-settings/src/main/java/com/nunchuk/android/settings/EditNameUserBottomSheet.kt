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

package com.nunchuk.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.settings.databinding.BottomSheetEditNameBinding

class EditNameUserBottomSheet : BaseBottomSheet<BottomSheetEditNameBinding>() {

    var listener: (EditNameUserOption) -> Unit = {}

    private val userName: String
        get() = arguments?.getString(ARG_NAME).orEmpty()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetEditNameBinding {
        return BottomSheetEditNameBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
    }

    private fun setupViews() {
        binding.edtName.setText(userName)
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
        listener(EditNameUserOption.Save(binding.edtName.text.toString().trim()))
        dismiss()
    }

    companion object {
        private const val TAG = "EditNameUserBottomSheet"
        const val ARG_NAME = "ARG_NAME"
        fun show(name: String, fragmentManager: FragmentManager) = EditNameUserBottomSheet().apply {
            arguments = bundleOf(
                ARG_NAME to name
            )
            show(fragmentManager, TAG)
        }
    }
}

sealed class EditNameUserOption {
    data class Save(val name: String) : EditNameUserOption()
}