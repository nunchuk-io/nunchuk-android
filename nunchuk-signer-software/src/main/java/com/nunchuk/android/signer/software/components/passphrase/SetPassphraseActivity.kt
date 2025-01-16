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

package com.nunchuk.android.signer.software.components.passphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivitySetPassphraseBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetPassphraseActivity : BaseActivity<ActivitySetPassphraseBinding>() {
    override fun initializeBinding() = ActivitySetPassphraseBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, SetPassphraseFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }

    companion object {
        fun start(
            activityContext: Context,
            mnemonic: String,
            signerName: String,
            primaryKeyFlow: Int,
            passphrase: String,
            groupId: String? = null,
            replacedXfp: String? = null,
            walletId: String = "",
            signerIndex: Int,
        ) {
            activityContext.startActivity(
                Intent(activityContext, SetPassphraseActivity::class.java).putExtras(
                    SetPassphraseFragmentArgs(
                        mnemonic = mnemonic,
                        signerName = signerName,
                        primaryKeyFlow = primaryKeyFlow,
                        passphrase = passphrase,
                        groupId = groupId,
                        replacedXfp = replacedXfp.orEmpty(),
                        walletId = walletId,
                        index = signerIndex
                    ).toBundle()
                )
            )
        }
    }

}