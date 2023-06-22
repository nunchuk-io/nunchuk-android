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

package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownsuccess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LockdownSuccessFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: EmergencyLockdownSuccessViewModel by viewModels()
    private val args: LockdownSuccessFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                LockdownSuccessScreen(viewModel, args.period)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is EmergencyLockdownSuccessEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is EmergencyLockdownSuccessEvent.SignOut -> {
                    showOrHideLoading(loading = false)
                    NcToastManager.scheduleShowMessage(message = getString(R.string.nc_your_account_has_been_signed_out), delay = 500L)
                    navigator.restartApp(requireActivity())
                }
            }
        }
    }
}

@Composable
fun LockdownSuccessScreen(
    viewModel: EmergencyLockdownSuccessViewModel = viewModel(),
    period: String = ""
) {
    LockdownSuccessScreenContent(period) {
        viewModel.onContinueClicked()
    }
}

@Composable
fun LockdownSuccessScreenContent(
    period: String = "",
    onGotItClick: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(title = "", elevation = 0.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nc_green_stick),
                        contentDescription = ""
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_emergency_lockdown_success_title),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(id = R.string.nc_emergency_lockdown_success_info, period),
                    style = NunchukTheme.typography.body
                )

                Spacer(modifier = Modifier.weight(1.0f))

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onGotItClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_got_it))
                }
            }
        }
    }
}

@Preview
@Composable
private fun LockdownSuccessScreenContentPreview() {
    LockdownSuccessScreenContent()
}