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

package com.nunchuk.android.auth.components.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navArgs
import com.nunchuk.android.auth.R
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInAuthenticationActivity : BaseNfcActivity<ActivityNavigationBinding>() {
    private val args: SignInAuthenticationActivityArgs by navArgs()
    private val viewModel: SignInAuthenticationViewModel by viewModels()
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initStartDestination()
        observer()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.signin_authentication_navigation)
        graph.setStartDestination(R.id.signInDummyTransactionIntroFragment)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            if (it is SignInAuthenticationEvent.Loading) {
                showOrHideLoading(it.isLoading)
            } else if (it is SignInAuthenticationEvent.ShowError) {
                hideLoading()
                NCToastMessage(this).showError(it.message)
            }
        }
    }

    companion object {
        fun start(
            requiredSignatures: Int,
            launcher: ActivityResultLauncher<Intent>?,
            activityContext: Activity,
            dummyTransactionId: String? = null,
            signInData: String? = null
        ) {
            val intent = Intent(activityContext, SignInAuthenticationActivity::class.java).apply {
                putExtras(
                    SignInAuthenticationActivityArgs(
                        requiredSignatures = requiredSignatures,
                        dummyTransactionId = dummyTransactionId,
                        signInData = signInData
                    ).toBundle()
                )
            }
            launcher?.launch(intent) ?: activityContext.startActivity(intent)
        }
    }
}