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

package com.nunchuk.android.signer.tapsigner.backup.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UploadBackUpTapSignerFragment : MembershipFragment() {
    private val args: UploadBackUpTapSignerFragmentArgs by navArgs()
    private val viewModel: UploadBackUpTapSignerViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcViewModel.updateMasterSigner(args.masterSignerId)
        viewModel.init((requireActivity() as NfcSetupActivity).isAddNewSigner)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                UploadBackUpTapSignerScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.upload()
        flowObserver(viewModel.event) {
            when (it) {
                UploadBackUpTapSignerEvent.OnContinueClicked -> {
                    findNavController().navigate(
                        UploadBackUpTapSignerFragmentDirections.actionUploadBackUpTapSignerFragmentToTapSignerBackUpExplainFragment(
                            viewModel.getServerFilePath(),
                            args.masterSignerId
                        ),
                        NavOptions.Builder()
                            .setPopUpTo(findNavController().graph.startDestinationId, true).build()
                    )
                }
                is UploadBackUpTapSignerEvent.ShowError -> showError(it.message)
                is UploadBackUpTapSignerEvent.KeyVerified -> {
                    NcToastManager.scheduleShowMessage(it.message)
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
private fun UploadBackUpTapSignerScreen(
    viewModel: UploadBackUpTapSignerViewModel = viewModel(),
    membershipStepManager: MembershipStepManager
) {
    val state: UploadBackUpTapSignerState by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    UploadBackUpTapSignerContent(
        onContinueClicked = viewModel::onContinueClicked,
        percentage = state.percent,
        isError = state.isError,
        remainTime = remainTime,
    )
}

@Composable
private fun UploadBackUpTapSignerContent(
    onContinueClicked: () -> Unit = {},
    percentage: Int = 0,
    isError: Boolean = false,
    remainTime: Int = 0,
) = NunchukTheme {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_upload_back_up_tapsinger,
                title = stringResource(id = R.string.nc_estimate_remain_time, remainTime),
            )
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_back_up_tapsigner),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.nc_back_up_tap_signer_desc),
                style = NunchukTheme.typography.body
            )
            LinearProgressIndicator(
                progress = percentage.div(100f),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .height(8.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colors.primary,
                backgroundColor = colorResource(id = R.color.nc_whisper_color),
            )
            val label = when {
                isError -> stringResource(R.string.nc_upload_failed)
                percentage == 100 -> stringResource(R.string.nc_backup_uploaded_successfully)
                else -> "${percentage}%"
            }
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = label,
                style = NunchukTheme.typography.body.copy(
                    color = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.primary
                )
            )
            Spacer(modifier = Modifier.weight(1.0f))
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = percentage == 100 || isError,
                onClick = onContinueClicked,
            ) {
                Text(
                    text = if (isError) stringResource(R.string.nc_try_again)
                    else stringResource(R.string.nc_text_continue)
                )
            }
        }
    }
}

@Preview
@Composable
private fun UploadBackUpTapSignerScreenPreview() {
    UploadBackUpTapSignerContent(percentage = 75)
}

@Preview
@Composable
private fun UploadBackUpTapSignerScreenFailedPreview() {
    UploadBackUpTapSignerContent(percentage = 75, isError = true)
}

@Preview
@Composable
private fun UploadBackUpTapSignerScreen100Preview() {
    UploadBackUpTapSignerContent(percentage = 100, isError = false)
}
