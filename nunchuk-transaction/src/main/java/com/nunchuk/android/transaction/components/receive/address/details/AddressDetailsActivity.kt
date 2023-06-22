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

package com.nunchuk.android.transaction.components.receive.address.details

import android.content.Context
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.transaction.databinding.ActivityAddressDetailsBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddressDetailsActivity : BaseActivity<ActivityAddressDetailsBinding>() {

    private val args: AddressDetailsArgs by lazy { AddressDetailsArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityAddressDetailsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.qrCode.setImageBitmap(args.address.convertToQRCode())
        binding.address.text = args.address
        binding.balance.text = args.balance
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnClose.setOnClickListener { finish() }
    }

    companion object {
        fun start(activityContext: Context, address: String, balance: String) {
            val intent = AddressDetailsArgs(address = address, balance = balance).buildIntent(activityContext)
            activityContext.startActivity(intent)
        }
    }

}