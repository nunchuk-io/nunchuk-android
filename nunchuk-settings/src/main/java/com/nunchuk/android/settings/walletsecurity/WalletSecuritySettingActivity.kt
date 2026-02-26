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

package com.nunchuk.android.settings.walletsecurity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.activity.enableEdgeToEdge
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.domain.membership.PasswordVerificationHelper
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalletSecuritySettingActivity : BaseComposeActivity() {
    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    @Inject
    lateinit var passwordVerificationHelper: PasswordVerificationHelper

    val args: WalletSecurityArgs by lazy {
        intent.extras?.let { extras ->
            WalletSecurityArgs.fromBundle(extras)
        } ?: WalletSecurityArgs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(
            ComposeView(this).apply {
                setContent {
                    WalletSecuritySettingNavHost(
                        args = args,
                        activity = this@WalletSecuritySettingActivity,
                        navigator = navigator,
                        signInModeHolder = signInModeHolder,
                        passwordVerificationHelper = passwordVerificationHelper,
                    )
                }
            },
        )
    }

    companion object {

        fun start(activityContext: Context, args: WalletSecurityArgs) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    WalletSecuritySettingActivity::class.java
                ).apply {
                    putExtras(args.buildBundle())
                }
            )
        }
    }
}
