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

package com.nunchuk.android.signer.software.components.primarykey.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.network.NetworkVerifier
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.databinding.ActivityPkeySignUpIntroBinding
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PKeySignUpIntroActivity : BaseActivity<ActivityPkeySignUpIntroBinding>() {

    @Inject
    lateinit var networkVerifier: NetworkVerifier

    override fun initializeBinding() = ActivityPkeySignUpIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.btnGotIt.setOnDebounceClickListener {
            if (networkVerifier.isConnected()) {
                navigator.openAddPrimaryKeyScreen(
                    this,
                    primaryKeyFlow = PrimaryKeyFlow.SIGN_UP
                )
            } else {
                NCInfoDialog(this).init(
                    title = getString(R.string.nc_device_offline),
                    message = getString(R.string.nc_device_offline_primary_key_desc)
                ).show()
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }


    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    PKeySignUpIntroActivity::class.java
                )
            )
        }
    }
}