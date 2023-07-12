/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.main.membership.byzantine.key

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.wallet.components.cosigning.CosigningGroupPolicyFragmentArgs

class ConfigureByzantineServerKeySettingFragment : MembershipFragment() {
    private val viewModel: ConfigureByzantineServerKeySettingViewModel by viewModels()
    private val args: ConfigureByzantineServerKeySettingFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ConfigureServerKeySettingScreen(
                    viewModel = viewModel,
                    membershipStepManager = membershipStepManager,
                    isCreateAssistedWalletFlow = isCreateAssistedWalletFlow,
                    onMoreClicked = ::handleShowMore
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is ConfigureByzantineServerKeySettingEvent.ConfigServerSuccess -> handleConfigServerKeySuccess()
                ConfigureByzantineServerKeySettingEvent.NoDelayInput -> showError(getString(R.string.nc_error_co_signing_delay_empty))
                is ConfigureByzantineServerKeySettingEvent.ShowError -> showError(it.message)
                ConfigureByzantineServerKeySettingEvent.DelaySigningInHourInvalid -> showError(
                    getString(R.string.nc_delay_signing_invalid)
                )

                is ConfigureByzantineServerKeySettingEvent.Loading -> showOrHideLoading(it.isLoading)
                is ConfigureByzantineServerKeySettingEvent.EditGroupServerKey -> handleEditConfigGroupServerKey(
                    it.keyPolicy
                )
            }
        }
    }

    private fun handleEditConfigGroupServerKey(keyPolicy: GroupKeyPolicy) {
        requireActivity().apply {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtras(
                    CosigningGroupPolicyFragmentArgs(
                        keyPolicy = keyPolicy,
                    ).toBundle()
                )
            })
            finish()
        }
    }

    private fun handleConfigServerKeySuccess() {
        requireActivity().finish()
    }

    private val isCreateAssistedWalletFlow: Boolean
        get() = args.xfp.isNullOrEmpty()
}

@Composable
fun ConfigureServerKeySettingScreen(
    viewModel: ConfigureByzantineServerKeySettingViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    isCreateAssistedWalletFlow: Boolean,
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConfigureServerKeySettingScreenContent(
        state = state,
        onContinueClicked = viewModel::onContinueClicked,
        remainTime = remainTime,
        isCreateAssistedWalletFlow = isCreateAssistedWalletFlow,
        onCoSigningDelaHourTextChange = viewModel::updateCoSigningDelayHourText,
        onCoSigningDelaMinuteTextChange = viewModel::updateCoSigningDelayMinuteText,
        onAutoBroadcastSwitchedChange = viewModel::updateAutoBroadcastSwitched,
        onEnableCoSigningSwitchedChange = viewModel::updateEnableCoSigningSwitched,
        onMoreClicked = onMoreClicked,
    )
}

@Composable
fun ConfigureServerKeySettingScreenContent(
    state: ConfigureServerKeySettingState = ConfigureServerKeySettingState.Empty,
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    remainTime: Int = 0,
    isCreateAssistedWalletFlow: Boolean = false,
    onCoSigningDelaHourTextChange: (value: String) -> Unit = {},
    onCoSigningDelaMinuteTextChange: (value: String) -> Unit = {},
    onAutoBroadcastSwitchedChange: (checked: Boolean) -> Unit = {},
    onEnableCoSigningSwitchedChange: (checked: Boolean) -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                NcTopAppBar(
                    if (isCreateAssistedWalletFlow) stringResource(
                        R.string.nc_estimate_remain_time,
                        remainTime
                    ) else "",
                    elevation = 0.dp,
                    actions = {
                        if (isCreateAssistedWalletFlow) {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        }
                    }
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_configure_server_key_setting_cosigning_delay),
                    style = NunchukTheme.typography.heading
                )
                Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
                    Text(
                        text = stringResource(id = R.string.nc_configure_server_key_setting_auto_broadcast),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        style = NunchukTheme.typography.body
                    )
                    Switch(
                        checked = state.autoBroadcastSwitched,
                        onCheckedChange = onAutoBroadcastSwitchedChange,
                        colors = SwitchDefaults.colors(

                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 24.dp, end = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_configure_server_key_setting_enable_cosigning_title),
                            style = NunchukTheme.typography.body
                        )
                        Text(
                            text = stringResource(id = R.string.nc_configure_server_key_setting_enable_cosigning_desc),
                            modifier = Modifier
                                .padding(top = 4.dp),
                            style = NunchukTheme.typography.bodySmall,
                            color = NcColor.greyDark
                        )
                    }

                    Switch(
                        checked = state.enableCoSigningSwitched,
                        onCheckedChange = {
                            onEnableCoSigningSwitchedChange(it)
                        }
                    )
                }
                AnimatedVisibility(visible = state.enableCoSigningSwitched) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        NcTextField(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .weight(1f),
                            title = stringResource(id = R.string.nc_hours),
                            value = state.cosigningTextHours,
                            onValueChange = onCoSigningDelaHourTextChange,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        NcTextField(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f),
                            title = stringResource(id = R.string.nc_minutes),
                            value = state.cosigningTextMinutes,
                            onValueChange = onCoSigningDelaMinuteTextChange,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_configure_server_key_setting_info)))
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(
                        text = if (isCreateAssistedWalletFlow)
                            stringResource(R.string.nc_text_continue)
                        else stringResource(R.string.nc_update_cosigning_delay)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CoSigningDelayInput(isVisible: Boolean = true) {

}

@Preview
@Composable
private fun ConfigureServerKeySettingScreenPreview() {
    ConfigureServerKeySettingScreenContent()
}
