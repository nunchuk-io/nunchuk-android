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

package com.nunchuk.android.signer.software.components.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseActivity
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
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, CreateNewSeedFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            keyFlow: Int,
            passphrase: String,
            walletId: String,
            groupId: String?,
            replacedXfp: String? = null,
            numberOfWords: Int,
            backupHotKeySignerId: String
        ) {
            activityContext.startActivity(
                buildIntent(
                    activityContext,
                    keyFlow,
                    passphrase,
                    walletId,
                    groupId,
                    replacedXfp,
                    numberOfWords,
                    backupHotKeySignerId
                )
            )
        }

        fun buildIntent(
            activityContext: Context,
            keyFlow: Int = 0,
            passphrase: String = "",
            walletId: String = "",
            groupId: String? = null,
            replacedXfp: String? = null,
            numberOfWords: Int = 24,
            backupHotKeySignerId: String = ""
        ): Intent = Intent(
            activityContext,
            CreateNewSeedActivity::class.java
        ).apply {
            putExtras(
                CreateNewSeedFragmentArgs(
                    isQuickWallet = false,
                    primaryKeyFlow = keyFlow,
                    passphrase = passphrase,
                    walletId = walletId,
                    groupId = groupId,
                    replacedXfp = replacedXfp.orEmpty(),
                    numberOfWords = numberOfWords,
                    backupHotKeySignerId = backupHotKeySignerId
                ).toBundle()
            )
        }
    }
}