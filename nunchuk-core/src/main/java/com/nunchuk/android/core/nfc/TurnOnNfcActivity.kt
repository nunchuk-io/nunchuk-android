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

package com.nunchuk.android.core.nfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.databinding.ActivityTurnOnNfcBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TurnOnNfcActivity : BaseActivity<ActivityTurnOnNfcBinding>() {
    private val requestEnableNfc =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (NfcAdapter.getDefaultAdapter(this).isEnabled) {
                setResult(RESULT_OK)
                finish()
            }
        }

    override fun initializeBinding(): ActivityTurnOnNfcBinding = ActivityTurnOnNfcBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerEvents()
    }

    private fun registerEvents() {
        binding.btnGotIt.setOnClickListener {
            try {
                requestEnableNfc.launch(Intent(Settings.ACTION_NFC_SETTINGS))
            } catch (ignore: Exception) {
            }
        }
        binding.toolbar.setNavigationOnClickListener {
           finish()
        }
    }
}