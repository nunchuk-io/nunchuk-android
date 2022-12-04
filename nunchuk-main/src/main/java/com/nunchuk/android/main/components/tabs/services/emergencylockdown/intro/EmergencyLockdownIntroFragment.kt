package com.nunchuk.android.main.components.tabs.services.emergencylockdown.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R

class EmergencyLockdownIntroFragment : Fragment() {

    private val viewModel: EmergencyLockdownIntroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                EmergencyLockdownIntroScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is EmergencyLockdownIntroEvent.ContinueClick -> {
                    findNavController().navigate(EmergencyLockdownIntroFragmentDirections.actionEmergencyLockdownIntroFragmentToLockdownPeriodFragment())
                }
                is EmergencyLockdownIntroEvent.Loading -> {

                }
                else -> {}
            }
        }
    }
}

@Composable
fun EmergencyLockdownIntroScreen(
    viewModel: EmergencyLockdownIntroViewModel = viewModel()
) {
    EmergencyLockdownIntroScreenContent(onContinueClicked = {
        viewModel.onContinueClicked()
    })
}

@Composable
fun EmergencyLockdownIntroScreenContent(
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_key_recovery
                )
                Text(
                    modifier = Modifier.padding(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    text = stringResource(R.string.nc_emergency_lockdown),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    text = stringResource(R.string.nc_emergency_lockdown_intro_desc),
                    style = NunchukTheme.typography.body
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 1,
                    label = stringResource(R.string.nc_emergency_lockdown_intro_info_1)
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 2,
                    label = stringResource(R.string.nc_emergency_lockdown_intro_info_2)
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 3,
                    label = stringResource(R.string.nc_emergency_lockdown_intro_info_3)
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun EmergencyLockdownIntroScreenPreview() {
    EmergencyLockdownIntroScreenContent()
}