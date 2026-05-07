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

package com.nunchuk.android.wallet.personal.components.stablecoin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.signer.SelectSignerArgs
import com.nunchuk.android.core.signer.SelectSignerBottomSheet
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.wallet.personal.components.stablecoin.intro.StablecoinIntroScreenRoute
import com.nunchuk.android.wallet.personal.components.stablecoin.intro.stablecoinIntroScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StablecoinWalletActivity : BaseComposeActivity() {

    private val viewModel: StablecoinWalletViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NunchukTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                var showSelectSignerSheet by rememberSaveable { mutableStateOf(false) }
                val navController = rememberNavController()

                NavHost(
                    modifier = Modifier.fillMaxSize(),
                    navController = navController,
                    startDestination = StablecoinIntroScreenRoute,
                ) {
                    stablecoinIntroScreen(
                        onContinueClicked = {
                            if (state.softwareSigners.isNotEmpty()) {
                                showSelectSignerSheet = true
                            } else {
                                // TODO: navigate to create a new software key
                            }
                        },
                    )
                }

                if (showSelectSignerSheet) {
                    SelectSignerBottomSheet(
                        args = SelectSignerArgs(
                            signers = state.softwareSigners,
                            type = SignerType.SOFTWARE,
                        ),
                        onDismiss = { showSelectSignerSheet = false },
                        onAddExistKey = { _ ->
                            showSelectSignerSheet = false
                            // TODO: continue with the selected existing software key
                        },
                        onAddNewKey = {
                            showSelectSignerSheet = false
                            // TODO: navigate to create a new software key
                        },
                    )
                }
            }
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(activityContext, StablecoinWalletActivity::class.java)
            )
        }
    }
}
