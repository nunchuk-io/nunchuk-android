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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceKeyTipFragment : MembershipFragment() {
    private val viewModel: InheritanceKeyTipViewModel by viewModels()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
                val sharedState by inheritanceViewModel.state.collectAsStateWithLifecycle()
                InheritanceKeyTipContent(
                    remainTime = remainTime,
                    isMiniscriptWallet = sharedState.isMiniscriptWallet,
                    onContinueClicked = { viewModel.onContinueClick() }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                InheritanceKeyTipEvent.ContinueClickEvent -> {
                    findNavController().navigate(
                        InheritanceKeyTipFragmentDirections.actionInheritanceKeyTipFragmentToInheritanceActivationDateFragment()
                    )
                }
            }
        }
    }
}

@Composable
private fun InheritanceKeyTipContent(
    remainTime: Int = 0,
    isMiniscriptWallet: Boolean = false,
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritance_key_illustration,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
            },
            bottomBar = {
                Column {
                    if (isMiniscriptWallet) {
                        NcHintMessage(
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            messages = listOf(
                                com.nunchuk.android.core.util.ClickAbleText(
                                    stringResource(R.string.nc_inheritance_key_hardware_device_hint)
                                )
                            )
                        )
                    }

                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_inheritance_key_tip),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(16.dp),
                    text = if (isMiniscriptWallet) {
                        stringResource(R.string.nc_inheritance_key_tip_desc_miniscript)
                    } else {
                        stringResource(R.string.nc_inheritance_key_tip_desc)
                    },
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenPreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        isMiniscriptWallet = false,
        onContinueClicked = {}
    )
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenMiniscriptPreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        isMiniscriptWallet = true,
        onContinueClicked = {}
    )
}