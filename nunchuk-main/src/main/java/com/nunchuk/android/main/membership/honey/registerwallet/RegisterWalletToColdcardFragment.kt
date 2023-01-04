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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.wallet.components.upload.SharedWalletConfigurationViewModel
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterWalletToColdcardFragment : MembershipFragment() {
    private val viewModel: RegisterWalletToColdcardViewModel by viewModels()
    private val sharedViewModel by activityViewModels<SharedWalletConfigurationViewModel>()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            openNextScreen()
        }

    private val args: RegisterWalletToColdcardFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel.init(args.walletId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RegisterWalletToColdcardScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        RegisterWalletToColdcardEvent.ExportWalletToColdcard -> {
                            BottomSheetOption.newInstance(
                                title = getString(R.string.nc_select_your_export_method),
                                options = listOf(
                                    SheetOption(
                                        type = SheetOptionType.EXPORT_COLDCARD_VIA_NFC,
                                        resId = R.drawable.ic_nfc_indicator_small,
                                        stringId = R.string.nc_export_via_nfc
                                    ),
                                    SheetOption(
                                        type = SheetOptionType.EXPORT_COLDCARD_VIA_FILE,
                                        resId = R.drawable.ic_export,
                                        stringId = R.string.nc_export_via_file
                                    )
                                ),
                                showClosedIcon = true
                            ).show(childFragmentManager, "BottomSheetOption")
                        }
                    }
                }
        }

        sharedViewModel.event.observe(viewLifecycleOwner) {
            if (it is UploadConfigurationEvent.ExportColdcardSuccess) {
                if (it.filePath.isNullOrEmpty()) {
                    openNextScreen()
                } else {
                    IntentSharingController.from(
                        activityContext = requireActivity(),
                        launcher = launcher
                    ).shareFile(it.filePath.orEmpty())
                }
            }
        }
    }

    private fun openNextScreen() {
        viewModel.setRegisterColdcardSuccess()
        if (args.hasAirgap) {
            findNavController().navigate(
                RegisterWalletToColdcardFragmentDirections.actionRegisterWalletToColdcardFragmentToRegisterWalletToAirgapFragment(
                    args.walletId
                ),
            )
        } else {
            findNavController().navigate(
                RegisterWalletToColdcardFragmentDirections.actionRegisterWalletFragmentToCreateWalletSuccessFragment(
                    args.walletId
                ),
            )
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun RegisterWalletToColdcardScreen(viewModel: RegisterWalletToColdcardViewModel = viewModel()) {
    val remainingTime by viewModel.remainTime.collectAsStateWithLifecycle()
    RegisterWalletToColdcardContent(
        remainingTime = remainingTime,
        onExportToColdcardClicked = viewModel::onExportColdcardClicked
    )
}

@Composable
private fun RegisterWalletToColdcardContent(
    remainingTime: Int = 0,
    onExportToColdcardClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_register_coldcard,
                    title = stringResource(id = R.string.nc_estimate_remain_time, remainingTime),
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_register_wallet_to_coldcard),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_register_wallet_to_coldcard_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onExportToColdcardClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_wallet_export_coldcard))
                }
            }
        }
    }
}

@Preview
@Composable
private fun RegisterWalletScreenPreview() {
    RegisterWalletToColdcardContent()
}