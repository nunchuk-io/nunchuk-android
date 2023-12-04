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

package com.nunchuk.android.settings.walletsecurity.createpin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.settings.R
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletSecurityCreatePinFragment : Fragment() {

    private val viewModel: WalletSecurityCreatePinViewModel by viewModels()
    private val args: WalletSecurityCreatePinFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WalletSecurityCreatePinScreen(viewModel, args.currentPin.isBlank())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is WalletSecurityCreatePinEvent.Error -> NCToastMessage(requireActivity()).showError(
                    message = event.message
                )
                is WalletSecurityCreatePinEvent.Loading -> showOrHideLoading(loading = event.loading)
                WalletSecurityCreatePinEvent.CreateOrUpdateSuccess -> {
                    val message = if (args.currentPin.isBlank()) {
                        getString(R.string.nc_pin_created)
                    } else {
                        getString(R.string.nc_pin_updated)
                    }
                    NcToastManager.scheduleShowMessage(message)
                    findNavController().popBackStack()
                }
            }
        }
    }
}

@Composable
fun WalletSecurityCreatePinScreen(
    viewModel: WalletSecurityCreatePinViewModel = viewModel(),
    createPinFlow: Boolean = true
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val inputTitleArray = if (createPinFlow) {
        arrayListOf(
            stringResource(id = R.string.nc_enter_pin),
            stringResource(id = R.string.nc_confirm_pin)
        )
    } else {
        arrayListOf(
            stringResource(id = R.string.nc_enter_current_pin),
            stringResource(id = R.string.nc_enter_new_pin),
            stringResource(id = R.string.nc_confirm_new_pin),
        )
    }
    WalletSecurityCreatePinContent(
        createPinFlow = createPinFlow,
        inputTitleArray = inputTitleArray,
        inputValue = state.inputValue,
        onInputChange = { index, value ->
            viewModel.updateInputValue(index, value)
        }, onContinueClick = {
            viewModel.createOrUpdateWalletPin()
        })
}

@Composable
private fun WalletSecurityCreatePinContent(
    createPinFlow: Boolean = true,
    inputTitleArray: ArrayList<String> = arrayListOf(),
    inputValue: MutableMap<Int, InputValue> = hashMapOf(),
    onContinueClick: () -> Unit = {},
    onInputChange: (Int, String) -> Unit = { _, _ -> },
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                val title = if (createPinFlow) stringResource(R.string.nc_create_a_wallet_pin) else stringResource(R.string.nc_change_wallet_pin)
                NcTopAppBar(
                    title = title,
                    textStyle = NunchukTheme.typography.titleLarge
                )
                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_create_a_wallet_pin_desc),
                    style = NunchukTheme.typography.body
                )
                inputTitleArray.forEachIndexed { index, title ->
                    NcTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .padding(horizontal = 16.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Number),
                        title = title,
                        value = inputValue[index]?.value.orEmpty(),
                        error = inputValue[index]?.errorMsg,
                        onValueChange = {
                            onInputChange(index, it)
                        },
                    )
                }

                val isButtonEnable = inputValue.all { it.value.value.isBlank().not() }
                if (createPinFlow) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp),
                        enabled = isButtonEnable,
                        onClick = onContinueClick,
                    ) {
                        Text(text = stringResource(id = R.string.nc_create_pin))
                    }
                } else {
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                            .height(48.dp),
                        enabled = isButtonEnable,
                        onClick = onContinueClick,
                    ) {
                        Text(text = stringResource(R.string.nc_change_wallet_pin))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun WalletSecurityCreatePinScreenPreview() {
    WalletSecurityCreatePinContent(
        inputTitleArray = arrayListOf(
            stringResource(id = R.string.nc_enter_pin),
            stringResource(id = R.string.nc_confirm_pin)
        )
    )
}