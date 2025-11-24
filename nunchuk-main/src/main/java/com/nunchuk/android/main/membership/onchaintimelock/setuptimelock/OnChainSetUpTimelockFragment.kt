package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

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
import androidx.compose.foundation.layout.height
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcDatePickerDialog
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTimePickerDialog
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.timezone.NcTimeZoneField
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.core.ui.toTimeZoneDetail
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R
import com.nunchuk.android.model.TimelockExtra
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone as JavaTimeZone

@AndroidEntryPoint
class OnChainSetUpTimelockFragment : MembershipFragment(), BottomSheetOptionListener {
    private val viewModel: OnChainSetUpTimelockViewModel by viewModels()
    private val args: OnChainSetUpTimelockFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                OnChainSetUpTimelockScreen(
                    viewModel = viewModel,
                    groupId = args.groupId,
                    timelockExtra = args.timelockExtra,
                    isReplaceKeyFlow = args.isReplaceKeyFlow,
                    walletId = args.walletId,
                    onMoreClicked = ::handleShowMore
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvent()
    }

    private fun observeEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is OnChainSetUpTimelockEvent.Success -> {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }

                        is OnChainSetUpTimelockEvent.Error -> {
                            NCToastMessage(requireActivity()).showError(event.message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnChainSetUpTimelockScreen(
    viewModel: OnChainSetUpTimelockViewModel = viewModel(),
    groupId: String? = null,
    timelockExtra: TimelockExtra? = null,
    isReplaceKeyFlow: Boolean = false,
    walletId: String? = null,
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val showConfirmDialog by viewModel.showConfirmTimelockDateDialog.collectAsStateWithLifecycle()
    val showInvalidDateDialog by viewModel.showInvalidDateDialog.collectAsStateWithLifecycle()
    val maxTimelockYears by viewModel.maxTimelockYears.collectAsStateWithLifecycle()

    OnChainSetUpTimelockContent(
        onMoreClicked = onMoreClicked,
        onContinueClicked = { selectedDate, selectedTimeZone ->
            viewModel.onContinueClick(
                selectedDate,
                selectedTimeZone,
                groupId = groupId,
                isReplaceKeyFlow = isReplaceKeyFlow,
                walletId = walletId
            )
        },
        remainTime = remainTime,
        timelockExtra = timelockExtra,
        showConfirmDialog = showConfirmDialog,
        showInvalidDateDialog = showInvalidDateDialog,
        maxTimelockYears = maxTimelockYears,
        isReplaceKeyFlow = isReplaceKeyFlow,
        onConfirmTimelockDate = { viewModel.onConfirmTimelockDate() },
        onDismissConfirmDialog = { viewModel.onDismissConfirmTimelockDateDialog() },
        onDismissInvalidDateDialog = { viewModel.onDismissInvalidDateDialog() }
    )
}

@Composable
private fun OnChainSetUpTimelockContent(
    remainTime: Int = 0,
    timelockExtra: TimelockExtra? = null,
    isReplaceKeyFlow: Boolean = false,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: (Calendar, TimeZoneDetail) -> Unit = { _, _ -> },
    showConfirmDialog: Boolean = false,
    showInvalidDateDialog: Boolean = false,
    maxTimelockYears: Int? = null,
    onConfirmTimelockDate: () -> Unit = {},
    onDismissConfirmDialog: () -> Unit = {},
    onDismissInvalidDateDialog: () -> Unit = {},
) {
    var selectedTimeZone by remember {
        mutableStateOf(
            timelockExtra?.timezone?.toTimeZoneDetail()
                ?: JavaTimeZone.getDefault().id.toTimeZoneDetail() ?: TimeZoneDetail()
        )
    }

    var selectedDate by remember {
        mutableStateOf(
            if (timelockExtra != null && timelockExtra.value > 0) {
                Calendar.getInstance(JavaTimeZone.getTimeZone(selectedTimeZone.id)).apply {
                    timeInMillis = timelockExtra.value * 1000 // Convert seconds to milliseconds
                }
            } else {
                Calendar.getInstance(JavaTimeZone.getTimeZone(selectedTimeZone.id)).apply {
                    add(Calendar.YEAR, 5)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        )
    }

    val selectedDateText by remember(selectedDate) {
        derivedStateOf {
            val month = selectedDate.get(Calendar.MONTH) + 1
            val day = selectedDate.get(Calendar.DAY_OF_MONTH)
            val year = selectedDate.get(Calendar.YEAR)
            String.format(Locale.getDefault(), "%02d/%02d/%04d", month, day, year)
        }
    }
    val selectedTimeText by remember(selectedDate) {
        derivedStateOf {
            val hour = selectedDate.get(Calendar.HOUR_OF_DAY)
            val minute = selectedDate.get(Calendar.MINUTE)
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        }
    }

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
        }, bottomBar = {
            Column {
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = "Even with the inheritance key, the Beneficiary cannot claim funds before the timelock expires."))
                )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        onContinueClicked(selectedDate, selectedTimeZone)
                    },
                ) {
                    Text(text = stringResource(id = R.string.nc_text_save))
                }
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    text = if (isReplaceKeyFlow) "Change on-chain timelock" else "Set up an on-chain timelock",
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(16.dp),
                    text = "Your inheritance plan includes an on-chain timelock enforced by the Bitcoin network. After it expires, the Beneficiary can claim the funds autonomously using the required secrets.\n" +
                            "\n" +
                            "[B]On-chain timelocks are immutable.[/B] Once set, the date cannot be changed. To adjust the date later, you must create a new wallet and migrate your funds. A guided feature is available in the Wallet Config screen.\n" +
                            "\n" +
                            "[B]Recommendation: Choose a longer timeframe.[/B] To minimize the need for future migrations, we recommend setting the timelock further into the future (e.g., 5 to 10 years).",
                    style = NunchukTheme.typography.body
                )

                // Timezone field
                NcTimeZoneField(
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp),
                    selectedTimeZone = selectedTimeZone,
                    onTimeZoneSelected = { timeZone ->
                        val year = selectedDate.get(Calendar.YEAR)
                        val month = selectedDate.get(Calendar.MONTH)
                        val day = selectedDate.get(Calendar.DAY_OF_MONTH)
                        val hour = selectedDate.get(Calendar.HOUR_OF_DAY)
                        val minute = selectedDate.get(Calendar.MINUTE)

                        selectedTimeZone = timeZone
                        selectedDate =
                            Calendar.getInstance(JavaTimeZone.getTimeZone(timeZone.id)).apply {
                                set(year, month, day, hour, minute, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                    },
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
                            NcIcon(
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
                            NcIcon(
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
                            val newCalendar =
                                Calendar.getInstance(JavaTimeZone.getTimeZone(selectedTimeZone.id))
                                    .apply {
                                        timeInMillis = date
                                        set(
                                            Calendar.HOUR_OF_DAY,
                                            selectedDate.get(Calendar.HOUR_OF_DAY)
                                        )
                                        set(Calendar.MINUTE, selectedDate.get(Calendar.MINUTE))
                                    }
                            selectedDate = newCalendar
                            datePickerDialog = false
                        },
                        defaultDate = selectedDate.timeInMillis
                    )
                }

                // Time picker dialog
                if (timePickerDialog) {
                    NcTimePickerDialog(
                        onDismissRequest = { timePickerDialog = false },
                        initialHour = selectedDate.get(Calendar.HOUR_OF_DAY),
                        initialMinute = selectedDate.get(Calendar.MINUTE),
                        onConfirm = { hour, minute ->
                            val newCalendar =
                                Calendar.getInstance(JavaTimeZone.getTimeZone(selectedTimeZone.id))
                                    .apply {
                                        timeInMillis = selectedDate.timeInMillis
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                    }
                            selectedDate = newCalendar
                            timePickerDialog = false
                        }
                    )
                }


                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Confirm timelock date dialog
    if (showConfirmDialog && maxTimelockYears != null) {
        NcConfirmationDialog(
            title = stringResource(id = R.string.nc_confirm_timelock_date_title),
            message = stringResource(
                id = R.string.nc_confirm_timelock_date_message,
                maxTimelockYears
            ),
            positiveButtonText = stringResource(id = com.nunchuk.android.widget.R.string.nc_text_confirm),
            negativeButtonText = stringResource(id = R.string.nc_change_date),
            onPositiveClick = onConfirmTimelockDate,
            onDismiss = onDismissConfirmDialog
        )
    }

    // Invalid date dialog
    if (showInvalidDateDialog) {
        NcInfoDialog(
            title = stringResource(id = R.string.nc_invalid_date_title),
            message = stringResource(id = R.string.nc_invalid_date_message),
            positiveButtonText = stringResource(id = com.nunchuk.android.widget.R.string.nc_text_got_it),
            onDismiss = onDismissInvalidDateDialog,
            onPositiveClick = onDismissInvalidDateDialog
        )
    }
}

@Preview
@Composable
private fun OnChainSetUpTimelockScreenPreview() {
    OnChainSetUpTimelockContent(
        remainTime = 0,
        timelockExtra = null,
        onMoreClicked = {},
        onContinueClicked = { _, _ -> }
    )
}