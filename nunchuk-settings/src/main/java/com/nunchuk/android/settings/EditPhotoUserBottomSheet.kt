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
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.settings.databinding.BottomSheetEditUserPhotoBinding

internal class EditPhotoUserBottomSheet : BaseBottomSheet<BottomSheetEditUserPhotoBinding>() {

    lateinit var listener: (EditPhotoOption) -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetEditUserPhotoBinding {
        return BottomSheetEditUserPhotoBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        binding.btnChooseAlbum.setOnClickListener { onSaveClicked(EditPhotoOption.SelectAlbum) }
        binding.btnTakePhoto.setOnClickListener { onSaveClicked(EditPhotoOption.TakePhoto) }
        binding.btnRemovePhoto.setOnClickListener { onSaveClicked(EditPhotoOption.RemovePhoto) }
        binding.btnRemovePhoto.isVisible = false // currently, we don't need remove photo feature.
    }

    private fun onSaveClicked(option: EditPhotoOption) {
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

sealed class EditPhotoOption {
    object SelectAlbum : EditPhotoOption()
    object TakePhoto : EditPhotoOption()
    object RemovePhoto : EditPhotoOption()
}


