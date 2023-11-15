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

@file:OptIn(ExperimentalFoundationApi::class)

package com.nunchuk.android.signer.tapsigner.backup.verify.byapp

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.widget.NCWarningVerticalDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CheckBackUpByAppFragment : MembershipFragment() {
    private val viewModel: CheckBackUpByAppViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                CheckBackUpByAppScreen(
                    viewModel,
                    membershipStepManager,
                    nfcViewModel.masterSignerId,
                    (activity as NfcSetupActivity).groupId
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        CheckBackUpByAppEvent.OnVerifyBackUpKeySuccess -> handleBackUpKeySuccess()
                        CheckBackUpByAppEvent.OnVerifyFailedTooMuch -> handleVerifyFailedTooMuch()
                        is CheckBackUpByAppEvent.ShowError -> if (nfcViewModel.handleNfcError(event.e)
                                .not()
                        ) {
                            showError(event.e?.message?.orUnknownError())
                        }
                        is CheckBackUpByAppEvent.GetTapSignerBackupKeyEvent -> {
                            nfcViewModel.masterSigner?.let { masterSigner ->
                                findNavController().navigate(
                                    CheckBackUpByAppFragmentDirections.actionCheckBackUpByAppFragmentToUploadBackUpTapSignerFragment(
                                        event.filePath,
                                        masterSigner.id
                                    )
                                )
                            }
                        }
                        is CheckBackUpByAppEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
                    }
                }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_VIEW_BACKUP_KEY }) { nfcScanInfo ->
            viewModel.getTapSignerBackup(
                IsoDep.get(nfcScanInfo.tag) ?: return@flowObserver,
                nfcViewModel.masterSignerId,
                nfcViewModel.inputCvc.orEmpty()
            )
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleBackUpKeySuccess() {
        findNavController().navigate(CheckBackUpByAppFragmentDirections.actionCheckBackUpByAppFragmentToBackUpResultHealthyFragment())
    }

    private fun handleVerifyFailedTooMuch() {
        NCWarningVerticalDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_error),
            message = getString(R.string.nc_verify_back_up_failed_too_much_desc),
            btnYes = getString(R.string.nc_keep_try_different_keys),
            btnNo = getString(R.string.nc_reupload_backup_file),
            onNoClick = {
                (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_VIEW_BACKUP_KEY)
            }
        )
    }
}

@Composable
private fun CheckBackUpByAppScreen(
    viewModel: CheckBackUpByAppViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    masterSignerId: String,
    groupId: String,
) {
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

    CheckBackUpByAppContent(
        onContinueClicked = viewModel::onContinueClicked,
        decryptionKey = viewModel.decryptionKey,
        errorMessage = viewModel.errorMessage,
        onValueChange = viewModel::onDecryptionKeyChange,
        remainingTime = remainingTime,
        masterSignerId = masterSignerId,
        groupId = groupId,
    )
}

@Composable
private fun CheckBackUpByAppContent(
    onContinueClicked: (groupId: String, masterSignerId: String) -> Unit = {_, _ ->},
    decryptionKey: String = "",
    errorMessage: String = "",
    onValueChange: (value: String) -> Unit = {},
    remainingTime: Int = 0,
    masterSignerId: String = "",
    groupId: String = "",
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .imePadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_tap_signer_explain,
                    title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_verify_backup_via_the_app_title),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_verify_back_up_app_desc),
                    style = NunchukTheme.typography.body
                )
                NcTextField(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    onFocusEvent = { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                delay(500L)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                    error = errorMessage,
                    title = stringResource(id = R.string.nc_backup_password),
                    value = decryptionKey,
                    onValueChange = onValueChange
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .fillMaxWidth(),
                    onClick = { onContinueClicked(groupId, masterSignerId) }
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun CheckBackUpByAppScreenPreview() {
    CheckBackUpByAppContent(

    )
}
