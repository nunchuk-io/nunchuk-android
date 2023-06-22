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

package com.nunchuk.android.main.components.tabs.services.emergencylockdown.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R

class EmergencyLockdownIntroFragment : Fragment() {

    private val viewModel: EmergencyLockdownIntroViewModel by viewModels()
    private val args: EmergencyLockdownIntroFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

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
                    findNavController().navigate(
                        EmergencyLockdownIntroFragmentDirections.actionEmergencyLockdownIntroFragmentToLockdownPeriodFragment(
                            verifyToken = args.verifyToken
                        )
                    )
                }
                is EmergencyLockdownIntroEvent.Loading -> {

                }
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
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        NcImageAppBar(
                            backgroundRes = R.drawable.emergency_lockdown_illustrations
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
                    }
                }

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