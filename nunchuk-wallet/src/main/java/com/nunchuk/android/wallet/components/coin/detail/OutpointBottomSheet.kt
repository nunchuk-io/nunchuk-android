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

package com.nunchuk.android.wallet.components.coin.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.TextUtils
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.FragmentDialogOutpointBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OutpointBottomSheet : BaseBottomSheet<FragmentDialogOutpointBinding>() {
    @Inject
    lateinit var textUtils: TextUtils

    override fun initializeBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentDialogOutpointBinding {
        return FragmentDialogOutpointBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val outpoint = requireArguments().getString(EXTRA_OUTPOINT).orEmpty()
        binding.tvOutpointValue.text = outpoint
        binding.btnCopy.setOnClickListener {
            textUtils.copyText(text = outpoint)
            binding.btnCopy.text = getString(R.string.nc_copied)
        }
    }

    companion object {
        private const val EXTRA_OUTPOINT = "extra_outpoint"

        fun newInstance(outpoint: String): OutpointBottomSheet {
            return OutpointBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_OUTPOINT, outpoint)
                }
            }
        }
    }
}