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

package com.nunchuk.android.main.membership.honey.registerwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.wallet.components.base.BaseWalletConfigActivity
import com.nunchuk.android.wallet.components.upload.SharedWalletConfigurationViewModel
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterWalletToAirgapFragment : MembershipFragment() {
    private val viewModel: RegisterWalletToAirgapViewModel by viewModels()
    private val sharedViewModel by activityViewModels<SharedWalletConfigurationViewModel>()

    private val args: RegisterWalletToAirgapFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel.init(args.walletId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RegisterWalletToAirgapScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    (requireActivity() as BaseWalletConfigActivity<*>).showSubOptionsExportQr()
                }
        }

        sharedViewModel.event.observe(viewLifecycleOwner) {
            if (it == UploadConfigurationEvent.DoneScanQr) {
                viewModel.setRegisterAirgapSuccess()
                findNavController().navigate(
                    RegisterWalletToAirgapFragmentDirections.actionRegisterWalletToAirgapFragmentToCreateWalletSuccessFragment(
                        args.walletId
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun RegisterWalletToAirgapScreen(viewModel: RegisterWalletToAirgapViewModel = viewModel()) {
    val remainingTime by viewModel.remainTime.collectAsStateWithLifecycle()
    RegisterWalletToAirgapContent(
        remainingTime = remainingTime,
        onExportToColdcardClicked = viewModel::onExportColdcardClicked
    )
}

@Composable
private fun RegisterWalletToAirgapContent(
    remainingTime: Int = 0,
    onExportToColdcardClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).navigationBarsPadding()) {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_register_to_air_gapped,
                    title = stringResource(id = R.string.nc_estimate_remain_time, remainingTime),
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_register_wallet_to_airgap_title),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_register_wallet_to_airgap_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onExportToColdcardClicked,
                ) {
                    Text(text = stringResource(R.string.nc_show_wallet_qr))
                }
            }
        }
    }
}

@Preview
@Composable
private fun RegisterWalletScreenPreview() {
    RegisterWalletToAirgapContent()
}