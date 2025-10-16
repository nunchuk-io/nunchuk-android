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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceNoteFragment : MembershipFragment() {

    private val viewModel: InheritanceNoteViewModel by viewModels()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()
    private val args: InheritanceNoteFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceNoteScreen(viewModel, args, inheritanceViewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceNoteEvent.ContinueClick -> {
                    inheritanceViewModel.setOrUpdate(
                        inheritanceViewModel.setupOrReviewParam.copy(
                            note = event.note
                        )
                    )
                    if (args.isUpdateRequest || inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                        findNavController().popBackStack()
                    } else if (inheritanceViewModel.isMiniscriptWallet()) {
                        findNavController().navigate(
                            InheritanceNoteFragmentDirections.actionInheritanceNoteFragmentToInheritanceNotifyPrefFragment(
                                isUpdateRequest = args.isUpdateRequest
                            )
                        )
                    } else {
                        findNavController().navigate(
                            InheritanceNoteFragmentDirections.actionInheritanceNoteFragmentToInheritanceBufferPeriodFragment()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InheritanceNoteScreen(
    viewModel: InheritanceNoteViewModel = viewModel(),
    args: InheritanceNoteFragmentArgs,
    inheritanceViewModel: InheritancePlanningViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()

    InheritanceNoteScreenContent(
        remainTime = remainTime,
        note = state.note,
        planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
        isUpdateRequest = args.isUpdateRequest,
        onContinueClick = viewModel::onContinueClicked,
        onTextChange = viewModel::updateNote
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InheritanceNoteScreenContent(
    remainTime: Int = 0,
    note: String = "",
    planFlow: Int = InheritancePlanFlow.NONE,
    isUpdateRequest: Boolean = false,
    onContinueClick: () -> Unit = {},
    onTextChange: (value: String) -> Unit = {}
) {
    val isSetupFlow = planFlow == InheritancePlanFlow.SETUP && isUpdateRequest.not()
    val title = if (isSetupFlow) stringResource(
        id = R.string.nc_estimate_remain_time,
        remainTime
    ) else ""
    val continueBtnText =
        if (isSetupFlow) stringResource(id = R.string.nc_text_continue) else stringResource(
            id = R.string.nc_update_message
        )
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = title,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    },
                )
            },
            bottomBar = {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClick,
                    ) {
                        Text(text = continueBtnText)
                    }
                    if (isSetupFlow) {
                        NcOutlineButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            onClick = onContinueClick,
                        ) {
                            Text(text = stringResource(R.string.nc_text_skip))
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_inheritance_leave_message),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_inheritance_leave_message_desc),
                    style = NunchukTheme.typography.body
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 4.dp),
                        text = stringResource(id = R.string.nc_note),
                        style = NunchukTheme.typography.titleSmall
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                        text = stringResource(id = R.string.nc_optional),
                        style = NunchukTheme.typography.bodySmall
                    )
                }

                NcTextField(
                    value = note,
                    onValueChange = onTextChange,
                    title = "",
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    singleLine = false,
                    minLines = 5,
                    keyboardOptions = KeyboardOptions.Default,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceNoteScreenPreview() {
    InheritanceNoteScreenContent()
}