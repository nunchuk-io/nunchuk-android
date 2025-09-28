package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcDatePickerDialog
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTimePickerDialog
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.timezone.NcTimeZoneField
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.core.ui.toTimeZoneDetail
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class OnChainSetUpTimelockFragment : MembershipFragment(), BottomSheetOptionListener {
    private val viewModel: OnChainSetUpTimelockViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                OnChainSetUpTimelockScreen(viewModel)
            }
        }
    }
}

@Composable
private fun OnChainSetUpTimelockScreen(
    viewModel: OnChainSetUpTimelockViewModel = viewModel(),
    onMoreClicked: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    
    OnChainSetUpTimelockContent(
        onMoreClicked = onMoreClicked,
        onContinueClicked = onContinue,
        remainTime = remainTime
    )
}

@Composable
private fun OnChainSetUpTimelockContent(
    remainTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    // Local state management
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedTimeZone by remember { 
        mutableStateOf(TimeZone.getDefault().id.toTimeZoneDetail() ?: TimeZoneDetail())
    }
    
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val selectedDateText by remember { derivedStateOf { dateFormat.format(selectedDate.time) } }
    val selectedTimeText by remember { derivedStateOf { timeFormat.format(selectedDate.time) } }
    
    var datePickerDialog by remember { mutableStateOf(false) }
    var timePickerDialog by remember { mutableStateOf(false) }

    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ),
                actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    text = "Set up an on-chain timelock",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Your inheritance plan includes an on-chain timelock that protects your funds. After it expires, the Beneficiary can claim the funds using the Magic Phrase and the inheritance key.\n" +
                            "\n" +
                            "You can change the timelock later in the Services tab by creating a new wallet and transferring the funds. The process will be guided automatically.\n" +
                            "\n" +
                            "We recommend configuring a timelock of one to three years for security and flexibility. Review the plan annually and adjust the date if necessary to avoid long-term uncertainties.",
                    style = NunchukTheme.typography.body
                )

                // Timezone field
                NcTimeZoneField(
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp),
                    selectedTimeZone = selectedTimeZone,
                    onTimeZoneSelected = { timeZone ->
                        selectedTimeZone = timeZone
                    }
                )

                // Date and Time fields
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 10.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date field
                    NcTextField(
                        modifier = Modifier.weight(1f),
                        title = "Date",
                        value = selectedDateText,
                        readOnly = true,
                        enabled = false,
                        onClick = {
                            datePickerDialog = true
                        },
                        rightContent = {
                            Icon(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        datePickerDialog = true
                                    },
                                painter = painterResource(id = com.nunchuk.android.core.R.drawable.ic_calendar),
                                contentDescription = ""
                            )
                        },
                        onValueChange = {}
                    )
                    
                    // Time field
                    NcTextField(
                        modifier = Modifier.weight(1f),
                        title = "Time",
                        value = selectedTimeText,
                        readOnly = true,
                        enabled = false,
                        onClick = {
                            timePickerDialog = true
                        },
                        rightContent = {
                            Icon(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        timePickerDialog = true
                                    },
                                painter = painterResource(id = com.nunchuk.android.core.R.drawable.ic_clock),
                                contentDescription = ""
                            )
                        },
                        onValueChange = {}
                    )
                }

                // Date picker dialog
                if (datePickerDialog) {
                    NcDatePickerDialog(
                        onDismissRequest = { datePickerDialog = false },
                        onConfirm = { date ->
                            val newCalendar = Calendar.getInstance().apply {
                                timeInMillis = date
                                // Preserve existing time when date changes
                                set(Calendar.HOUR_OF_DAY, selectedDate.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, selectedDate.get(Calendar.MINUTE))
                            }
                            selectedDate = newCalendar
                            datePickerDialog = false
                        }
                    )
                }

                // Time picker dialog
                if (timePickerDialog) {
                    NcTimePickerDialog(
                        onDismissRequest = { timePickerDialog = false },
                        initialHour = selectedDate.get(Calendar.HOUR_OF_DAY),
                        initialMinute = selectedDate.get(Calendar.MINUTE),
                        onConfirm = { hour, minute ->
                            val newCalendar = Calendar.getInstance().apply {
                                timeInMillis = selectedDate.timeInMillis
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                            }
                            selectedDate = newCalendar
                            timePickerDialog = false
                        }
                    )
                }

                
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnChainSetUpTimelockScreenPreview() {
    OnChainSetUpTimelockContent(
        remainTime = 0,
        onMoreClicked = {},
        onContinueClicked = {}
    )
}