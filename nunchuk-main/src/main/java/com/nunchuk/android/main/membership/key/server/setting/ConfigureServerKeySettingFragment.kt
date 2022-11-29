package com.nunchuk.android.main.membership.key.server.setting

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyFragmentArgs

class ConfigureServerKeySettingFragment : MembershipFragment() {
    private val viewModel: ConfigureServerKeySettingViewModel by viewModels()
    private val args: ConfigureServerKeySettingFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ConfigureServerKeySettingScreen(
                    viewModel,
                    membershipStepManager,
                    isCreateAssistedWalletFlow
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is ConfigureServerKeySettingEvent.ConfigServerSuccess -> handleConfigServerKeySuccess(
                    it.keyPolicy
                )
                ConfigureServerKeySettingEvent.NoDelayInput -> showError(getString(R.string.nc_error_co_signing_delay_empty))
                is ConfigureServerKeySettingEvent.ShowError -> showError(it.message)
                ConfigureServerKeySettingEvent.DelaySigningInHourInvalid -> showError(getString(R.string.nc_delay_signing_invalid))
                is ConfigureServerKeySettingEvent.Loading -> showOrHideLoading(it.isLoading)
            }
        }
    }

    private fun handleConfigServerKeySuccess(keyPolicy: KeyPolicy) {
        if (isCreateAssistedWalletFlow) {
            findNavController().popBackStack(
                R.id.addKeyListFragment,
                false
            )
        } else {
            requireActivity().apply {
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtras(
                        CosigningPolicyFragmentArgs(
                            keyPolicy,
                            xfp = args.xfp.orEmpty()
                        ).toBundle()
                    )
                })
                finish()
            }
        }
    }

    private val isCreateAssistedWalletFlow: Boolean
        get() = args.xfp.isNullOrEmpty()
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun ConfigureServerKeySettingScreen(
    viewModel: ConfigureServerKeySettingViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    isCreateAssistedWalletFlow: Boolean,
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConfigureServerKeySettingScreenContent(
        state = state,
        onContinueClicked = viewModel::onContinueClicked,
        remainTime = remainTime,
        isCreateAssistedWalletFlow = isCreateAssistedWalletFlow,
        onCoSigningDelaTextChange = viewModel::updateCoSigningDelayText,
        onAutoBroadcastSwitchedChange = viewModel::updateAutoBroadcastSwitched,
        onEnableCoSigningSwitchedChange = viewModel::updateEnableCoSigningSwitched,
    )
}

@Composable
fun ConfigureServerKeySettingScreenContent(
    state: ConfigureServerKeySettingState = ConfigureServerKeySettingState.Empty,
    onContinueClicked: () -> Unit = {},
    remainTime: Int = 0,
    isCreateAssistedWalletFlow: Boolean = false,
    onCoSigningDelaTextChange: (value: String) -> Unit = {},
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
            ) {
                NcTopAppBar(
                    if (isCreateAssistedWalletFlow) stringResource(
                        R.string.nc_estimate_remain_time,
                        remainTime
                    ) else "",
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
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
                    NcTextField(
                        modifier = Modifier.padding(16.dp),
                        title = stringResource(id = R.string.nc_cosigning_delay),
                        value = state.cosigningText,
                        onValueChange = onCoSigningDelaTextChange,
                        rightText = stringResource(id = R.string.nc_hours),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
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
