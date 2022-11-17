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

package com.nunchuk.android.signer.software.components.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityCreateSeedBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateNewSeedActivity : BaseActivity<ActivityCreateSeedBinding>() {
    override fun initializeBinding(): ActivityCreateSeedBinding {
        return ActivityCreateSeedBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        val primaryKeyFlow = intent.getIntExtra(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)
        val passphrase = intent.getStringExtra(EXTRA_PASSPHRASE).orEmpty()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, CreateNewSeedFragment().apply {
                    arguments = CreateNewSeedFragmentArgs(
                        isQuickWallet = false,
                        primaryKeyFlow = primaryKeyFlow,
                        passphrase = passphrase
                    ).toBundle()
                })
            }
        }
    }

    companion object {
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"

        fun start(activityContext: Context, primaryKeyFlow: Int, passphrase: String) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    CreateNewSeedActivity::class.java
                ).apply {
                    putExtra(
                        EXTRA_PRIMARY_KEY_FLOW,
                        primaryKeyFlow
                    )
                    putExtra(
                        EXTRA_PASSPHRASE,
                        passphrase
                    )
                })
        }
    }
}