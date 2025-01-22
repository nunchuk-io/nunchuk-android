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

package com.nunchuk.android.signer.software.components.confirm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityConfirmSeedBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmSeedActivity : BaseActivity<ActivityConfirmSeedBinding>() {

    override fun initializeBinding(): ActivityConfirmSeedBinding {
        return ActivityConfirmSeedBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, ConfirmSeedFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }

    companion object {
        fun buildIntent(
            activityContext: Context,
            mnemonic: String,
            passphrase: String,
            primaryKeyFlow: Int,
            masterSignerId: String,
            walletId: String,
            groupId: String? = null,
            replacedXfp: String? = null,
        ) = Intent(
            activityContext,
            ConfirmSeedActivity::class.java
        ).putExtras(
            ConfirmSeedFragmentArgs(
                mnemonic = mnemonic,
                primaryKeyFlow = primaryKeyFlow,
                passphrase = passphrase,
                masterSignerId = masterSignerId,
                walletId = walletId,
                groupId = groupId,
                replacedXfp = replacedXfp.orEmpty(),
            ).toBundle()
        )
    }
}