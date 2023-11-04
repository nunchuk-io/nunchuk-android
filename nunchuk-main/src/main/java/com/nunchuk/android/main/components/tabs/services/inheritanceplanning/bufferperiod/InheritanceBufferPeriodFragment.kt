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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.model.Period
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InheritanceBufferPeriodFragment : MembershipFragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: InheritanceBufferPeriodViewModel by viewModels()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()
    private val args: InheritanceBufferPeriodFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceBufferPeriodScreen(viewModel, args, inheritanceViewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceBufferPeriodEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is InheritanceBufferPeriodEvent.Error -> showError(message = event.message)
                is InheritanceBufferPeriodEvent.OnContinueClick -> {
                    inheritanceViewModel.setOrUpdate(inheritanceViewModel.setupOrReviewParam.copy(bufferPeriod = event.period))
                    if (args.isUpdateRequest || inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                        setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(EXTRA_BUFFER_PERIOD to event.period)
                        )
                        findNavController().popBackStack()
                    } else {
                        findNavController().navigate(
                            InheritanceBufferPeriodFragmentDirections.actionInheritanceBufferPeriodFragmentToInheritanceNotifyPrefFragment()
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "InheritanceBufferPeriodFragment"
        const val EXTRA_BUFFER_PERIOD = "EXTRA_BUFFER_PERIOD"
    }
}

@Composable
private fun InheritanceBufferPeriodScreen(
    viewModel: InheritanceBufferPeriodViewModel = viewModel(),
    args: InheritanceBufferPeriodFragmentArgs,
    inheritanceViewModel: InheritancePlanningViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()

    InheritanceBufferPeriodContent(
        remainTime = remainTime,
        planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
        options = state.options,
        isUpdateRequest = args.isUpdateRequest,
        onOptionClick = viewModel::onOptionClick,
        onContinueClick = viewModel::onContinueClick
    )
}

@Composable
private fun InheritanceBufferPeriodContent(
    remainTime: Int = 0,
    planFlow: Int = InheritancePlanFlow.NONE,
    isUpdateRequest: Boolean = false,
    options: List<BufferPeriodOption> = emptyList(),
    onOptionClick: (String) -> Unit = {},
    onContinueClick: () -> Unit = {}
) = NunchukTheme {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val isSetupFlow = planFlow == InheritancePlanFlow.SETUP && isUpdateRequest.not()
            val title = if (isSetupFlow) stringResource(
                id = R.string.nc_estimate_remain_time,
                remainTime
            ) else ""
            NcTopAppBar(title = title)
            LazyColumn(
                modifier = Modifier.weight(1F),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.nc_set_up_buffer_period),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(R.string.nc_set_up_buffer_period_desc),
                        style = NunchukTheme.typography.body,
                    )
                }
                items(options) { item ->
                    OptionItem(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = item.isSelected,
                        label = item.period.displayName,
                        isRecommended = item.period.isRecommended
                    ) {
                        onOptionClick(item.period.id)
                    }
                }
            }

            val continueBtnText =
                if (isSetupFlow) stringResource(id = R.string.nc_text_continue) else stringResource(
                    id = R.string.nc_update_buffer_period
                )
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = options.any { it.isSelected },
                onClick = onContinueClick,
            ) {
                Text(text = continueBtnText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier, onClick = onClick,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) colorResource(id = R.color.nc_primary_color) else Color(
                0xFFDEDEDE
            )
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick)
            Text(text = label, style = NunchukTheme.typography.title)
            if (isRecommended) {
                NcTag(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    label = stringResource(id = R.string.nc_recommended)
                )
            }
        }
    }
}

@Preview
@Composable
private fun InheritanceBufferPeriodFragmentContentPreview() {
    InheritanceBufferPeriodContent(options = listOf(
        BufferPeriodOption(period = Period(
            "", "", 3, true, "My Name", true
        ), false)
    ))
}

