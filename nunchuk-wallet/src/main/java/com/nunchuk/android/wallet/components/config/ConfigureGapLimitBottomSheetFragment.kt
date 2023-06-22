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

package com.nunchuk.android.wallet.components.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.hideKeyboard
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.BottomSheetConfigureGapLimitBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigureGapLimitBottomSheetFragment :
    BaseBottomSheet<BottomSheetConfigureGapLimitBinding>() {

    var listener: (Int) -> Unit = {}

    private val gapLimit: Int
        get() = arguments?.getInt(ARG_GAP_LIMIT) ?: 0

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetConfigureGapLimitBinding {
        return BottomSheetConfigureGapLimitBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()

    }

    private fun setupViews() {
        binding.errorText.text = String.format(getString(R.string.nc_gap_limit_error), LIMIT_GAP)
        binding.tvMax.text = String.format(getString(R.string.nc_max_data), LIMIT_GAP)
        binding.edtGapLimit.setText(gapLimit.toString())
        binding.closeBtn.setOnClickListener {
            binding.edtGapLimit.text?.clear()
            dismiss()
        }
        binding.saveBtn.setOnClickListener {
            val limit = binding.edtGapLimit.text.toString().toIntOrNull()
            if (limit != null && limit <= LIMIT_GAP) {
                listener(limit)
                dismiss()
            } else {
                binding.errorText.isVisible = true
                binding.edtGapLimit.hideKeyboard()
            }
        }
    }

    private fun cleanUp() {
        binding.edtGapLimit.text?.clear()
        dismiss()
    }

    companion object {
        const val LIMIT_GAP = 100

        private const val ARG_GAP_LIMIT = "ARG_GAP_LIMIT"
        fun show(gapLimit: Int, fragmentManager: FragmentManager) =
            ConfigureGapLimitBottomSheetFragment().apply {
                arguments = bundleOf(
                    ARG_GAP_LIMIT to gapLimit
                )
                show(fragmentManager, "ConfigureGapLimitBottomSheetFragment")
            }
    }
}