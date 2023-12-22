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

package com.nunchuk.android.signer.tapsigner.backup.verify.byself

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CheckBackUpBySelfFragment : MembershipFragment() {
    private val args: CheckBackUpBySelfFragmentArgs by navArgs()
    private val viewModel: CheckBackUpBySelfViewModel by viewModels()

    @Inject
    lateinit var masterSignerMapper: MasterSignerMapper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                CheckBackUpBySelfScreen(viewModel, membershipStepManager) {
                    requireActivity().openExternalLink("https://nunchuk.io/start")
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        OnExitSelfCheck -> requireActivity().finish()
                        OnDownloadBackUpClicked -> Unit
                        OnVerifiedBackUpClicked -> NCWarningDialog(requireActivity())
                            .showDialog(
                                title = getString(R.string.nc_confirmation),
                                message = getString(R.string.nc_confirm_verify_backup_by_self_desc),
                                onYesClick = {
                                    viewModel.setKeyVerified((requireActivity() as NfcSetupActivity).groupId)
                                },
                            )

                        is ShowError -> showError(event.e?.message.orUnknownError())
                        is GetBackUpKeySuccess -> IntentSharingController.from(requireActivity())
                            .shareFile(event.filePath)
                    }
                }
        }
    }
}

@Composable
private fun CheckBackUpBySelfScreen(
    viewModel: CheckBackUpBySelfViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    onLinkClicked: () -> Unit,
) {
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    CheckBackUpBySelfContent(
        onBtnClicked = viewModel::onBtnClicked,
        remainingTime = remainingTime,
        onLinkClicked = onLinkClicked,
    )
}

@Composable
private fun CheckBackUpBySelfContent(
    remainingTime: Int = 0,
    onBtnClicked: (event: CheckBackUpBySelfEvent) -> Unit = {},
    onLinkClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(stringResource(R.string.nc_estimate_remain_time, remainingTime))
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_verify_the_backup_yourself),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    text = stringResource(R.string.nc_verify_backup_yourself_desc),
                    style = NunchukTheme.typography.body,
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 1,
                    label = stringResource(R.string.nc_verify_backup_yourself_step_one)
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 2,
                    label = stringResource(R.string.nc_verify_backup_yourself_step_two)
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 3,
                    label = stringResource(R.string.nc_verify_backup_yourself_step_three)
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    messages = listOf(
                        ClickAbleText(content = stringResource(R.string.nc_self_verify_hint)),
                        ClickAbleText(content = "nunchuk.io/start", onLinkClicked)
                    ),
                    type = HighlightMessageType.HINT,
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = { onBtnClicked(OnDownloadBackUpClicked) },
                ) {
                    Text(text = stringResource(R.string.nc_download_backup_file))
                }
                NcOutlineButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    onClick = { onBtnClicked(OnVerifiedBackUpClicked) },
                ) {
                    Text(text = stringResource(R.string.nc_i_have_verified))
                }
                TextButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = { onBtnClicked(OnExitSelfCheck) },
                ) {
                    Text(text = stringResource(R.string.I_will_comeback_to_this_later))
                }
            }
        }
    }
}

@Preview
@Composable
private fun CheckBackUpBySelfScreenPreview() {
    CheckBackUpBySelfContent(

    )
}