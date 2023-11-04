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

package com.nunchuk.android.transaction.components.schedule

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.schedule.timezone.SelectTimeZoneFragment
import com.nunchuk.android.transaction.components.schedule.timezone.TimeZoneDetail
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.simpleGlobalDateFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@AndroidEntryPoint
class ScheduleBroadcastTransactionFragment : Fragment() {
    private val viewModel: ScheduleBroadcastTransactionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ScheduleBroadcastTransactionScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { event ->
                    when (event) {
                        ScheduleBroadcastTransactionEvent.OnSelectDateEvent -> showDatePicker()
                        ScheduleBroadcastTransactionEvent.OnSelectTimeEvent -> showTimePicker()
                        ScheduleBroadcastTransactionEvent.OnSelectTimeZoneEvent -> findNavController().navigate(
                            ScheduleBroadcastTransactionFragmentDirections.actionScheduleBroadcastTransactionFragmentToSelectTimeZoneFragment()
                        )
                        is ScheduleBroadcastTransactionEvent.ShowError -> showError(event.message)
                        is ScheduleBroadcastTransactionEvent.Loading -> showOrHideLoading(event.isLoading)
                        is ScheduleBroadcastTransactionEvent.ScheduleBroadcastSuccess -> {
                            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(
                                    ScheduleBroadcastTransactionActivity.EXTRA_SCHEDULE_BROADCAST_TIME,
                                    event.serverTransaction
                                )
                            })
                            requireActivity().finish()
                        }
                    }
                }
        }
        setFragmentResultListener(SelectTimeZoneFragment.REQUEST_KEY) { _, bundle ->
            val timeZone = bundle.parcelable<TimeZoneDetail>(SelectTimeZoneFragment.EXTRA_TIME_ZONE)
                ?: return@setFragmentResultListener
            viewModel.setTimeZone(timeZone)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = viewModel.state.value.time
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

    private fun showTimePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = viewModel.state.value.time
        }
        val dialog = TimePickerDialog(
            requireContext(), R.style.NunchukDateTimePicker, { _, hourOfDay, minute ->
                viewModel.setTime(hourOfDay, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false
        )
        dialog.show()
    }
}

@Composable
private fun ScheduleBroadcastTransactionScreen(viewModel: ScheduleBroadcastTransactionViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScheduleBroadcastTransactionContent(time = state.time,
        timeZone = state.timeZone,
        onDateSelect = {
            viewModel.onSelectEvent(ScheduleBroadcastTransactionEvent.OnSelectDateEvent)
        },
        onTimeSelect = {
            viewModel.onSelectEvent(ScheduleBroadcastTransactionEvent.OnSelectTimeEvent)
        },
        onTimeZoneSelect = {
            viewModel.onSelectEvent(ScheduleBroadcastTransactionEvent.OnSelectTimeZoneEvent)
        },
        onSaveClicked = viewModel::onSaveClicked
    )
}

@Composable
private fun ScheduleBroadcastTransactionContent(
    time: Long = 0,
    timeZone: TimeZoneDetail = TimeZoneDetail(),
    onDateSelect: () -> Unit = {},
    onTimeSelect: () -> Unit = {},
    onTimeZoneSelect: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                NcTopAppBar(title = "", isBack = false)
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    text = stringResource(id = R.string.nc_schedule_broadcast),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.nc_schedule_broadcast_desc),
                    style = NunchukTheme.typography.body
                )
                Row {
                    NcTextField(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 24.dp)
                            .weight(0.6f),
                        onClick = onDateSelect,
                        title = stringResource(R.string.nc_date),
                        enabled = false,
                        value = Date(time).simpleGlobalDateFormat(),
                        onValueChange = {},
                    )
                    NcTextField(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                            .weight(0.4f),
                        onClick = onTimeSelect,
                        title = stringResource(R.string.nc_time),
                        enabled = false,
                        value = Date(time).formatByHour(),
                        onValueChange = {},
                    )
                }
                NcTextField(
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp),
                    onClick = onTimeZoneSelect,
                    title = stringResource(R.string.nc_time_zone),
                    enabled = false,
                    value = timeZone.city,
                    rightContent = {
                        Icon(
                            modifier = Modifier
                                .padding(end = 12.dp),
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = ""
                        )
                    },
                    onValueChange = {},
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_schedule_broadcast_hint)))
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onSaveClicked,
                ) {
                    Text(stringResource(id = R.string.nc_text_save))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ScheduleBroadcastTransactionScreenPreview() {
    ScheduleBroadcastTransactionContent(

    )
}