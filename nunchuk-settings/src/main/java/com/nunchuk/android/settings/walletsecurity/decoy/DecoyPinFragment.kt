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

package com.nunchuk.android.settings.walletsecurity.decoy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPasswordTextField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DecoyPinFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = content {
        DecoyPinScreen(
            onContinueClick = { pin ->
                navigator.openAddWalletScreen(activityContext = requireContext(), decoyPin = pin)
            }
        )
    }
}

@Composable
fun DecoyPinScreen(
    viewModel: DecoyPinViewModel = viewModel(),
    onContinueClick: (String) -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    DecoyPinContent(
        uiState = uiState,
        onContinueClick = onContinueClick,
        getHashPin = viewModel::getHashedPin
    )
}

@Composable
private fun DecoyPinContent(
    uiState: DecoyPinUiState = DecoyPinUiState(),
    onContinueClick: (String) -> Unit = {},
    getHashPin: (String) -> String = { "" }
) {
    val context = LocalContext.current
    var pin by rememberSaveable { mutableStateOf("") }
    var pinErrorMsg by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var confirmPinErrorMsg by rememberSaveable { mutableStateOf("") }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(backgroundRes = R.drawable.bg_decoy_pin)
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = pin.isNotEmpty() && confirmPin.isNotEmpty(),
                    onClick = {
                        if (getHashPin(pin) == uiState.walletPin) {
                            pinErrorMsg = context.getString(R.string.nc_decoy_pin_same_as_wallet_pin)
                        } else if (pin != confirmPin) {
                            confirmPinErrorMsg =
                                context.getString(R.string.nc_confirm_pin_does_not_match)
                        } else {
                            onContinueClick(pin)
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = "Decoy Pin",
                    style = NunchukTheme.typography.heading
                )

                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_decoy_pin_desc),
                    style = NunchukTheme.typography.body,
                )
                NcPasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    title = "Your Decoy PIN",
                    value = pin,
                    error = pinErrorMsg,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    onValueChange = {
                        pin = it
                    },
                )
                NcPasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    title = "Confirm Your Decoy PIN",
                    value = confirmPin,
                    error = confirmPinErrorMsg,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    onValueChange = {
                        confirmPin = it
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                NcHintMessage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nc_decoy_pin_hint),
                        style = NunchukTheme.typography.caption
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DecoyPinScreenPreview() {
    DecoyPinContent()
}