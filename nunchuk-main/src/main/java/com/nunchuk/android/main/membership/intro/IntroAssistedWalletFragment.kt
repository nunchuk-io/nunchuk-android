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

package com.nunchuk.android.main.membership.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroAssistedWalletFragment : Fragment() {
    private val viewModel: IntroAssistedWalletViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                IntroAssistedWalletScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            val groupId = (activity as MembershipActivity).groupId
            if (groupId.isNotEmpty()) {
                findNavController().navigate(IntroAssistedWalletFragmentDirections.actionIntroAssistedWalletFragmentToAddGroupKeyStepFragment())
            } else {
                findNavController().navigate(IntroAssistedWalletFragmentDirections.actionIntroAssistedWalletFragmentToAddKeyStepFragment())
            }
        }
    }
}

@Composable
fun IntroAssistedWalletScreen(viewModel: IntroAssistedWalletViewModel = viewModel()) =
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = if (NunchukTheme.isDark) R.drawable.nc_bg_intro_assisted_wallet_dark else R.drawable.nc_bg_intro_assisted_wallet,
                    backIconRes = R.drawable.ic_close
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    onClick = { viewModel.onContinueClicked() }) {
                    Text(
                        text = stringResource(id = R.string.nc_text_continue),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_welcome_assisted_wallet_title),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_welcome_assisted_wallet_desc),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }

@PreviewLightDark
@Composable
fun IntroAssistedWalletScreenPreview() {
    IntroAssistedWalletScreen()
}
