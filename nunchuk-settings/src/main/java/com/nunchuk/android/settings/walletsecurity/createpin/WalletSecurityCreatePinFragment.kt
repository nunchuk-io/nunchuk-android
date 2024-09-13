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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPasswordTextField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.settings.R
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletSecurityCreatePinFragment : Fragment() {
    private val activityArgs by lazy {
        WalletSecurityArgs.fromBundle(requireActivity().intent.extras!!)
    }
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
                    if (activityArgs.type == WalletSecurityType.CREATE_PIN) {
                        val message = if (args.currentPin.isBlank()) {
                            getString(R.string.nc_pin_created)
                        } else {
                            getString(R.string.nc_pin_updated)
                        }
                        NcToastManager.scheduleShowMessage(message)
                        findNavController().popBackStack()
                    } else {
                        findNavController().navigate(
                            WalletSecurityCreatePinFragmentDirections.actionWalletSecurityCreatePinFragmentToDecoyPinFragment()
                        )
                    }
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
            stringResource(R.string.nc_your_pin),
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
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(backgroundRes = R.drawable.bg_wallet_pin)
            },
            bottomBar = {
                val isButtonEnable = inputValue.all { it.value.value.isBlank().not() }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = isButtonEnable,
                    onClick = onContinueClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = if (createPinFlow) "Security PIN" else "Change PIN",
                    style = NunchukTheme.typography.heading
                )

                if (createPinFlow) {
                    NcHighlightText(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(R.string.nc_create_a_wallet_pin_desc),
                        style = NunchukTheme.typography.body
                    )
                } else {
                    NcHighlightText(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(R.string.nc_be_sure_back_up_security_pin),
                        style = NunchukTheme.typography.body
                    )
                }
                inputTitleArray.forEachIndexed { index, title ->
                    NcPasswordTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        title = title,
                        value = inputValue[index]?.value.orEmpty(),
                        error = inputValue[index]?.errorMsg,
                        onValueChange = {
                            onInputChange(index, it)
                        },
                    )
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