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

package com.nunchuk.android.signer.tapsigner.id

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class TapSignerIdFragment : MembershipFragment() {
    private val viewModel: TapSignerIdViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    private val args: TapSignerIdFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                TapSignerIdScreen(viewModel, membershipStepManager, args)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { event ->
                when (event) {
                    is TapSignerIdEvent.GetTapSignerBackupKeyError -> if (nfcViewModel.handleNfcError(event.e).not()) {
                        val message = if (event.e is NCNativeException && event.e.message.contains("-6100")) {
                            getString(R.string.nc_card_id_does_not_match)
                        } else {
                            event.e?.message.orUnknownError()
                        }
                        showError(message)
                    }
                    is TapSignerIdEvent.GetTapSignerBackupKeyEvent -> handleBackUpKeySuccess(event.filePath)
                    is TapSignerIdEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
                    TapSignerIdEvent.OnContinueClicked -> (requireActivity() as NfcActionListener).startNfcFlow(
                        BaseNfcActivity.REQUEST_NFC_VIEW_BACKUP_KEY
                    )
                    TapSignerIdEvent.OnAddNewOne -> requireActivity().finish()
                }
            }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_VIEW_BACKUP_KEY }) {
            viewModel.getTapSignerBackup(
                IsoDep.get(it.tag) ?: return@flowObserver,
                nfcViewModel.inputCvc.orEmpty(),
            )
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleBackUpKeySuccess(filePath: String) {
        findNavController().navigate(
            TapSignerIdFragmentDirections.actionTapSignerIdFragmentToUploadBackUpTapSignerFragment(
                filePath = filePath,
                masterSignerId = args.masterSignerId
            )
        )
    }
}

@Composable
private fun TapSignerIdScreen(
    viewModel: TapSignerIdViewModel,
    membershipStepManager: MembershipStepManager,
    args: TapSignerIdFragmentArgs
) {
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    TapSignerIdContent(
        onContinueClicked = viewModel::onContinueClicked,
        onAddNewOneClicked = viewModel::onAddNewOneClicked,
        remainingTime = remainingTime,
        cardId = state.cardId,
        isExist = args.isExisted,
    )
}

@Composable
private fun TapSignerIdContent(
    remainingTime: Int = 0,
    onContinueClicked: () -> Unit = {},
    onAddNewOneClicked: () -> Unit = {},
    cardId: String = "",
    isExist: Boolean = false,
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                NcTopAppBar(stringResource(R.string.nc_estimate_remain_time, remainingTime))
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = if (isExist) stringResource(R.string.nc_tap_signer_already_existed) else stringResource(
                        R.string.nc_scan_your_card_title
                    ),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    text = if (isExist) stringResource(R.string.nc_tap_signer_existed_desc) else stringResource(
                        R.string.nc_scan_your_card_again_desc
                    ),
                    style = NunchukTheme.typography.body,
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                    text = stringResource(R.string.nc_existing_card_id),
                    style = NunchukTheme.typography.body,
                )
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                        .background(
                            color = colorResource(id = R.color.nc_grey_light),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = cardId,
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                if (isExist) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_yes_use_this_tapsigner))
                    }
                    NcOutlineButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(), onClick = onAddNewOneClicked
                    ) {
                        Text(text = stringResource(R.string.nc_no_add_a_new_one))
                    }
                } else {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TapSignerIdScreenPreview() {
    TapSignerIdContent(
        cardId = "ABCDE - 12345 - 12345 - ABCDE"
    )
}

@Preview
@Composable
private fun TapSignerIdScreenWithExistSignerPreview() {
    TapSignerIdContent(
        cardId = "ABCDE - 12345 - 12345 - ABCDE",
        isExist = true
    )
}