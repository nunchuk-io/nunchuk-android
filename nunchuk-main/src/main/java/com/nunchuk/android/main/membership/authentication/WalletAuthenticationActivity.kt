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

package com.nunchuk.android.main.membership.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navArgs
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.main.R
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletAuthenticationActivity : BaseNfcActivity<ActivityNavigationBinding>() {
    private val args: WalletAuthenticationActivityArgs by navArgs()
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.check_sign_message_navigation)

        when (args.type) {
            VerificationType.SIGN_TEMP_MESSAGE -> graph.setStartDestination(R.id.checkSignMessageFragment)
            VerificationType.SIGN_DUMMY_TX -> graph.setStartDestination(R.id.dummyTransactionIntroFragment)
            VerificationType.SECURITY_QUESTION -> graph.setStartDestination(R.id.answerSecurityQuestionFragment2)
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    companion object {
        fun start(
            walletId: String,
            userData: String,
            requiredSignatures: Int,
            type: String,
            launcher: ActivityResultLauncher<Intent>,
            activityContext: Activity,
            groupId: String? = null,
            dummyTransactionId: String? = null
        ) {
            launcher.launch(
                Intent(activityContext, WalletAuthenticationActivity::class.java).apply {
                    putExtras(
                        WalletAuthenticationActivityArgs(
                            walletId = walletId,
                            userData = userData,
                            requiredSignatures = requiredSignatures,
                            type = type,
                            groupId = groupId,
                            dummyTransactionId = dummyTransactionId
                        ).toBundle()
                    )
                }
            )
        }
    }
}