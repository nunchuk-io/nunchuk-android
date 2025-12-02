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

package com.nunchuk.android.main.membership.honey.registerwallet

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.args.BackUpWalletArgs
import com.nunchuk.android.nav.args.BackUpWalletType
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.wallet.components.base.BaseWalletConfigActivity
import com.nunchuk.android.wallet.components.upload.SharedWalletConfigurationViewModel
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterWalletToAirgapFragment : MembershipFragment() {
    private val viewModel: RegisterWalletToAirgapViewModel by viewModels()
    private val sharedViewModel by activityViewModels<SharedWalletConfigurationViewModel>()

    private val args: RegisterWalletToAirgapFragmentArgs by navArgs()

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val replacedWalletId = (activity as? com.nunchuk.android.main.membership.MembershipActivity)?.onChainReplaceWalletId.orEmpty()
            findNavController().navigate(
                RegisterWalletToAirgapFragmentDirections.actionRegisterWalletToAirgapFragmentToCreateWalletSuccessFragment(
                    walletId = args.walletId,
                    replacedWalletId = replacedWalletId
                ),
                NavOptions.Builder()
                    .setPopUpTo(findNavController().graph.startDestinationId, true)
                    .build()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel.init(args.walletId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                RegisterWalletToAirgapScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    val activity = requireActivity() as? BaseWalletConfigActivity<*>
                    val wallet = viewModel.wallet
                    val isMiniscriptWallet = viewModel.wallet?.miniscript?.isNotEmpty() == true
                    if (activity != null && wallet != null) {
                        activity.showExportQRTypeOption(wallet, isMiniscriptWallet)
                    }
                }
        }

        flowObserver(sharedViewModel.event) {
            val wallet = viewModel.wallet
            if (it == UploadConfigurationEvent.DoneScanQr) {
                viewModel.setRegisterAirgapSuccess(args.walletId)
                if (args.sendBsmsEmail) {
                    val replacedWalletId = (activity as? com.nunchuk.android.main.membership.MembershipActivity)?.onChainReplaceWalletId.orEmpty()
                    findNavController().navigate(
                        RegisterWalletToAirgapFragmentDirections.actionRegisterWalletToAirgapFragmentToCreateWalletSuccessFragment(
                            args.walletId,
                            replacedWalletId
                        )
                    )
                } else if (wallet != null) {
                    launcher.launch(
                        navigator.buildBackupWalletIntent(
                            activityContext = requireActivity(),
                            args = BackUpWalletArgs(
                                wallet = wallet,
                                backUpWalletType = BackUpWalletType.ASSISTED_CREATED
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisterWalletToAirgapScreen(viewModel: RegisterWalletToAirgapViewModel = viewModel()) {
    val remainingTime by viewModel.remainTime.collectAsStateWithLifecycle()
    RegisterWalletToAirgapContent(
        remainingTime = remainingTime,
        onExportToColdcardClicked = viewModel::onExportAirgapClicked
    )
}

@Composable
private fun RegisterWalletToAirgapContent(
    remainingTime: Int = 0,
    onExportToColdcardClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_register_to_air_gapped,
                title = if (remainingTime <= 0) "" else stringResource(id = R.string.nc_estimate_remain_time, remainingTime),
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
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