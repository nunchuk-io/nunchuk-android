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

package com.nunchuk.android.signer.software

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isSignInFlow
import com.nunchuk.android.signer.software.components.intro.recoverByXprv
import com.nunchuk.android.signer.software.components.intro.recoverByXprvRoute
import com.nunchuk.android.signer.software.components.intro.softwareSignerIntro
import com.nunchuk.android.signer.software.components.intro.softwareSignerIntroRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SoftwareSignerIntroActivity : BaseComposeActivity() {
    private val viewModel: SoftwareSignerIntroViewModel by viewModels()

    private val primaryKeyFlow: Int by lazy {
        intent.getIntExtra(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)
    }
    private val passphrase: String by lazy {
        intent.getStringExtra(EXTRA_PASSPHRASE).orEmpty()
    }
    private val groupId: String? by lazy {
        intent.getStringExtra(EXTRA_GROUP_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!replacedXfp.isNullOrEmpty()) {
            viewModel.getReplaceSignerName(walletId)
        }

        setContent {
            NunchukTheme {
                val navigationController = rememberNavController()
                NavHost(
                    navController = navigationController,
                    startDestination = softwareSignerIntroRoute
                ) {
                    softwareSignerIntro(
                        onCreateNewSeedClicked = ::openCreateNewSeedScreen,
                        onRecoverSeedClicked = ::openRecoverSeedScreen,
                        onRecoverXprvClicked = {
                            navigationController.navigate(recoverByXprvRoute)
                        }
                    )
                    recoverByXprv { xprv ->
                        onRecoverFromXprv(xprv)
                    }
                }
            }
        }
    }

    private fun onRecoverFromXprv(xprv: String) {
        when {
            primaryKeyFlow.isSignInFlow() -> {
                navigator.openPrimaryKeyEnterPassphraseScreen(
                    activityContext = this,
                    primaryKeyFlow = primaryKeyFlow,
                    xprv = xprv,
                )
            }

            !groupId.isNullOrEmpty() || !replacedXfp.isNullOrEmpty() -> {
                val signerName = if (replacedXfp.isNullOrEmpty()) {
                    viewModel.state.value.replaceSignerName
                } else {
                    "Key${viewModel.getSoftwareSignerName()}"
                }
                // TODO sync key or replace here
            }

            else -> {
                navigator.openAddSoftwareSignerNameScreen(
                    activityContext = this,
                    primaryKeyFlow = primaryKeyFlow,
                    passphrase = passphrase,
                    walletId = walletId,
                    xprv = xprv
                )
            }
        }
    }

    private fun openCreateNewSeedScreen() {
        navigator.openCreateNewSeedScreen(
            activityContext = this,
            passphrase = passphrase,
            primaryKeyFlow = primaryKeyFlow,
            groupId = groupId,
            replacedXfp = replacedXfp,
            walletId = walletId
        )
    }

    private fun openRecoverSeedScreen() {
        navigator.openRecoverSeedScreen(
            activityContext = this,
            passphrase = passphrase,
            primaryKeyFlow = primaryKeyFlow,
            groupId = groupId,
            replacedXfp = replacedXfp,
            walletId = walletId
        )
    }

    val replacedXfp: String? by lazy {
        intent.getStringExtra(EXTRA_REPLACED_XFP)
    }

    val walletId by lazy {
        intent.getStringExtra(EXTRA_WALLET_ID).orEmpty()
    }

    companion object {
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"
        private const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        private const val EXTRA_REPLACED_XFP = "EXTRA_REPLACED_XFP"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun start(
            activityContext: Context,
            passphrase: String,
            primaryKeyFlow: Int = PrimaryKeyFlow.NONE,
            groupId: String? = null,
            replacedXfp: String? = null,
            walletId: String = "",
        ) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    SoftwareSignerIntroActivity::class.java
                ).apply {
                    putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
                    putExtra(EXTRA_PASSPHRASE, passphrase)
                    groupId?.let { putExtra(EXTRA_GROUP_ID, it) }
                    replacedXfp?.let { putExtra(EXTRA_REPLACED_XFP, it) }
                    putExtra(EXTRA_WALLET_ID, walletId)
                },
            )
        }
    }
}