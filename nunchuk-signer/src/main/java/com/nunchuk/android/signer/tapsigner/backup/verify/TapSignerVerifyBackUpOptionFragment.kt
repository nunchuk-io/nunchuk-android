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

package com.nunchuk.android.signer.tapsigner.backup.verify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.backup.verify.components.TsVerifyBackUpOption
import com.nunchuk.android.signer.tapsigner.backup.verify.model.TsBackUpOption
import com.nunchuk.android.signer.tapsigner.backup.verify.model.TsBackUpOptionType
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TapSignerVerifyBackUpOptionFragment : MembershipFragment() {
    private val args: TapSignerVerifyBackUpOptionFragmentArgs by navArgs()
    private val viewModel: TapSignerVerifyBackUpOptionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TapSignerVerifyBackUpOptionScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                OnContinueClicked -> handleVerifyClicked()
            }
        }
    }

    private fun handleVerifyClicked() {
        when (viewModel.selectedOptionType) {
            TsBackUpOptionType.BY_APP -> {
                findNavController().navigate(
                    TapSignerVerifyBackUpOptionFragmentDirections.actionTapSignerVerifyBackUpOptionFragmentToCheckBackUpByAppFragment(
                        args.filePath
                    )
                )
            }

            TsBackUpOptionType.BY_MYSELF -> {
                findNavController().navigate(
                    TapSignerVerifyBackUpOptionFragmentDirections.actionTapSignerVerifyBackUpOptionFragmentToCheckBackUpBySelfFragment(
                        args.filePath,
                        args.masterSignerId
                    )
                )
            }

            TsBackUpOptionType.SKIP -> {
                NCWarningDialog(requireActivity()).showDialog(
                    title = getString(R.string.nc_confirmation),
                    message = getString(R.string.nc_skip_back_up_desc),
                    onYesClick = {
                        requireActivity().finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun TapSignerVerifyBackUpOptionScreen(
    viewModel: TapSignerVerifyBackUpOptionViewModel = viewModel(),
    membershipStepManager: MembershipStepManager
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

    TapSignerVerifyBackUpOptionContent(
        onContinueClicked = viewModel::onContinueClicked,
        onItemClicked = viewModel::onOptionClicked,
        options = options,
        remainingTime = remainingTime
    )
}

@Composable
private fun TapSignerVerifyBackUpOptionContent(
    onContinueClicked: () -> Unit = {},
    onItemClicked: (option: TsBackUpOption) -> Unit = {},
    remainingTime: Int = 0,
    options: List<TsBackUpOption> = emptyList(),
) = NunchukTheme {
    Scaffold(topBar = {
        NcTopAppBar(
            title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_verify_your_backup),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(R.string.nc_verify_back_up_desc),
                style = NunchukTheme.typography.body,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(options) { item ->
                    TsVerifyBackUpOption(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = item.isSelected,
                        label = stringResource(id = item.labelId),
                        isRecommend = item.type == TsBackUpOptionType.BY_MYSELF
                    ) {
                        onItemClicked(item)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1.0f))
            NcHintMessage(
                modifier = Modifier.padding(horizontal = 16.dp),
                messages = listOf(ClickAbleText(content = stringResource(R.string.nc_verify_backup_hint))),
                type = HighlightMessageType.HINT,
            )
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = options.any { it.isSelected },
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    }
}

@Preview
@Composable
private fun UploadBackUpTapSignerScreenPreview() {
    NunchukTheme {
        TapSignerVerifyBackUpOptionContent(
            options = TapSignerVerifyBackUpOptionViewModel.OPTIONS
        )
    }
}
