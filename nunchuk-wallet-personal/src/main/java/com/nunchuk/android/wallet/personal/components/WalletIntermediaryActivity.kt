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

package com.nunchuk.android.wallet.personal.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.databinding.ActivityWalletIntermediaryBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletIntermediaryActivity : BaseNfcActivity<ActivityWalletIntermediaryBinding>() {

    override fun initializeBinding(): ActivityWalletIntermediaryBinding {
        return ActivityWalletIntermediaryBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, WalletIntermediaryNewUIFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }
    companion object {
        const val REQUEST_CODE = 1111
        const val REQUEST_CODE_GROUP_WALLET = 1112

        const val EXTRA_HAS_SIGNER = "EXTRA_HAS_SIGNER"
        fun start(activityContext: Context, hasSigner: Boolean) {
            val intent = Intent(activityContext, WalletIntermediaryActivity::class.java).apply {
                putExtra(EXTRA_HAS_SIGNER, hasSigner)
            }
            activityContext.startActivity(intent)
        }
    }

}