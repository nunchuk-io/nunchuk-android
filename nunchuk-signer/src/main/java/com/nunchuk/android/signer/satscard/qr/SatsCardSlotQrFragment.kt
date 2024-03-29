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

package com.nunchuk.android.signer.satscard.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.signer.databinding.FragmentSatscardSlotQrBinding


class SatsCardSlotQrFragment : BaseFragment<FragmentSatscardSlotQrBinding>() {
    private val args: SatsCardSlotQrFragmentArgs by navArgs()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSatscardSlotQrBinding {
        return FragmentSatscardSlotQrBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        val width = resources.displayMetrics.widthPixels
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
        binding.qrCode.setImageBitmap(args.slot.address.orEmpty().convertToQRCode(width, width))
        binding.tvAddress.text = args.slot.address
        binding.tvBalanceBtc.text = args.slot.balance.getBTCAmount()
    }
}