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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.utils.simpleGlobalDateFormat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date

@AndroidEntryPoint
class InheritanceActivationDateFragment : MembershipFragment() {

    private val viewModel: InheritanceActivationDateViewModel by viewModels()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()
    private val args: InheritanceActivationDateFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceActivationDateScreen(viewModel,
                    args,
                    inheritanceViewModel,
                    onDatePicker = {
                        showDatePicker()
                    })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceActivationDateEvent.ContinueClick -> {
                    inheritanceViewModel.setOrUpdate(inheritanceViewModel.setupOrReviewParam.copy(activationDate = event.date))
                    if (args.isUpdateRequest || inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
                        findNavController().popBackStack()
                    } else {
                        findNavController().navigate(
                            InheritanceActivationDateFragmentDirections.actionInheritanceActivationDateFragmentToInheritanceNoteFragment()
                        )
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            val selectedDate = viewModel.state.value.date
            timeInMillis =
                if (selectedDate == 0L) Calendar.getInstance().timeInMillis else selectedDate
        }
        val dialog = DatePickerDialog(
            requireContext(), R.style.NunchukDateTimePicker,
            { _, year, month, dayOfMonth ->
                viewModel.setDate(year, month, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        )
        dialog.show()
    }

    companion object {
        const val REQUEST_KEY = "InheritanceActivationDateFragment"
        const val EXTRA_ACTIVATION_DATE = "EXTRA_ACTIVATION_DATE"
    }
}

@Composable
fun InheritanceActivationDateScreen(
    viewModel: InheritanceActivationDateViewModel = viewModel(),
    args: InheritanceActivationDateFragmentArgs,
    inheritanceViewModel: InheritancePlanningViewModel,
    onDatePicker: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val date = if (state.date > 0) Date(state.date).simpleGlobalDateFormat() else ""

    InheritanceActivationDateScreenContent(
        remainTime = remainTime,
        date = date,
        planFlow = inheritanceViewModel.setupOrReviewParam.planFlow,
        isUpdateRequest = args.isUpdateRequest,
        onContinueClick = {
            viewModel.onContinueClicked()
        }, onDatePick = {
            onDatePicker()
        })
}

@Composable
fun InheritanceActivationDateScreenContent(
    remainTime: Int = 0,
    date: String = "",
    planFlow: Int = InheritancePlanFlow.NONE,
    isUpdateRequest: Boolean = false,
    onContinueClick: () -> Unit = {},
    onDatePick: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val isSetupFlow = planFlow == InheritancePlanFlow.SETUP && isUpdateRequest.not()
                val title = if (isSetupFlow) stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ) else ""
                NcTopAppBar(title = title, actions = {
                    Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                })
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_set_up_activation_date),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_set_up_activation_date_desc),
                    style = NunchukTheme.typography.body
                )

                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    val (title, input) = createRefs()
                    Text(text = stringResource(id = R.string.nc_activation_date),
                        style = NunchukTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .constrainAs(title) {})
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.strokePrimary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = { onDatePick() })
                            .constrainAs(input) {
                                top.linkTo(title.bottom)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val textStyle =
                            if (date.isEmpty()) NunchukTheme.typography.body.copy(
                                color = NcColor.boulder
                            ) else NunchukTheme.typography.body
                        Text(
                            text = date
                                .ifBlank { stringResource(id = R.string.nc_activation_date_holder) },
                            style = textStyle,
                            modifier = Modifier
                                .padding(top = 14.dp, start = 12.dp, bottom = 14.dp)
                                .weight(1f)
                                .defaultMinSize(minWidth = TextFieldDefaults.MinWidth)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1.0f))

                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_set_up_activation_date_notice)))
                )
                val continueBtnText =
                    if (isSetupFlow) stringResource(id = R.string.nc_text_continue) else stringResource(
                        id = R.string.nc_update_activation_date
                    )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick,
                    enabled = date.isNotBlank()
                ) {
                    Text(text = continueBtnText)
                }
            }
        }
    }
}

@Preview
@Composable
private fun InheritanceActivationDateScreenPreview() {
    InheritanceActivationDateScreenContent()
}