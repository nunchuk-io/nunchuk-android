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
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.constants.Constants.MAIN_NET_HOST
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.hideKeyboard
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.BottomSheetConfigureGapLimitBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigureGapLimitBottomSheetFragment :
    BaseBottomSheet<BottomSheetConfigureGapLimitBinding>() {

    private val viewModel by viewModels<ConfigureGapLimitViewModel>()

    var listener: (Int) -> Unit = {}

    private val gapLimit: Int
        get() = arguments?.getInt(ARG_GAP_LIMIT) ?: 0

    private var isConnectUserServer: Boolean = false
    private var maxGapLimit = LIMIT_GAP

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

        flowObserver(viewModel.event) { event ->
            if (event is ConfigureGapLimitEvent.GetAppSettingSuccess) {
                if (event.url.isNotEmpty() && event.url != MAIN_NET_HOST) {
                    maxGapLimit = LIMIT_GAP_USER_SERVER
                    isConnectUserServer = true
                    updateInfoText()
                }
            }
        }
    }

    private fun setupViews() {
        updateInfoText()
        binding.edtGapLimit.setText(gapLimit.toString())
        binding.closeBtn.setOnClickListener {
            binding.edtGapLimit.text?.clear()
            dismiss()
        }
        binding.saveBtn.setOnClickListener {
            binding.errorText.isVisible = false
            binding.errorText.text = String.format(getString(R.string.nc_gap_limit_error), maxGapLimit)
            val limit = try {
                binding.edtGapLimit.text.toString().toInt()
            } catch (e: Exception) {
                binding.errorText.text = getString(R.string.nc_limit_too_large)
                null
            }
            if (limit != null && (limit <= maxGapLimit)) {
                listener(limit)
                dismiss()
            } else {
                binding.errorText.isVisible = true
                binding.edtGapLimit.hideKeyboard()
            }
        }
    }

    private fun updateInfoText() {
        binding.errorText.text = String.format(getString(R.string.nc_gap_limit_error), maxGapLimit)
        binding.tvMax.text = String.format(getString(R.string.nc_max_data), maxGapLimit)
    }

    companion object {
        const val LIMIT_GAP = 200
        const val LIMIT_GAP_USER_SERVER = 2000

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