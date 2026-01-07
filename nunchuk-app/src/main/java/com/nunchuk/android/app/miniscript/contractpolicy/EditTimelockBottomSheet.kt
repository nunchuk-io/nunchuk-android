package com.nunchuk.android.app.miniscript.contractpolicy

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcDatePickerDialog
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTimePickerDialog
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.core.ui.TimeZoneSelectionDialog
import com.nunchuk.android.core.ui.toTimeZoneDetail
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.type.MiniscriptTimelockType
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class TimelockData(
    val timelockType: MiniscriptTimelockType,
    val timeUnit: MiniscriptTimelockBased,
    val value: Long,
    // For absolute time locks: timezone information
    val timezoneId: String = "",
    val timezoneCity: String = "",
    val timezoneOffset: String = "",
    // For relative time locks: separate time components
    val days: Long = 0L,
    val hours: Long = 0L,
    val minutes: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimelockBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    currentBlockHeight: Long = 0L,
    initialData: TimelockData? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onDismiss: () -> Unit = {},
    onSave: (TimelockData) -> Unit = {}
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        dragHandle = {},
        content = {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .nestedScroll(rememberNestedScrollInteropConnection())
            ) {
                EditTimelockContent(
                    currentBlockHeight = currentBlockHeight,
                    initialData = initialData,
                    snackbarHostState = snackbarHostState,
                    onSave = onSave
                )
                NcSnackBarHost(snackbarHostState)
            }
        }
    )
}

@Composable
fun EditTimelockContent(
    currentBlockHeight: Long = 0L,
    initialData: TimelockData? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSave: (TimelockData) -> Unit = {}
) {
    var timelockType by remember {
        mutableStateOf(initialData?.timelockType ?: MiniscriptTimelockType.ABSOLUTE)
    }
    var timeUnit by remember {
        mutableStateOf(initialData?.timeUnit ?: MiniscriptTimelockBased.TIME_LOCK)
    }

    val calendar = remember {
        mutableStateOf(
            if (initialData != null &&
                initialData.timelockType == MiniscriptTimelockType.ABSOLUTE &&
                initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK
            ) {
                Calendar.getInstance().apply { timeInMillis = initialData.value * 1000 } // Convert seconds to milliseconds
            } else {
                Calendar.getInstance()
            }
        )
    }

    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val selectedDateText by remember { derivedStateOf { dateFormat.format(calendar.value.time) } }
    val selectedTimeText by remember { derivedStateOf { timeFormat.format(calendar.value.time) } }

    var selectedTimeZone by remember {
        mutableStateOf(
            if (initialData != null && 
                initialData.timelockType == MiniscriptTimelockType.ABSOLUTE && 
                initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK &&
                initialData.timezoneId.isNotEmpty()
            ) {
                TimeZoneDetail(
                    id = initialData.timezoneId,
                    city = initialData.timezoneCity,
                    offset = initialData.timezoneOffset
                )
            } else {
                TimeZone.getDefault().id.toTimeZoneDetail() ?: TimeZoneDetail()
            }
        )
    }

    // State for relative time fields
    var daysValue by remember {
        mutableStateOf(
            if (initialData != null &&
                initialData.timelockType == MiniscriptTimelockType.RELATIVE &&
                initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK
            ) {
                // Use the new days field if available, otherwise calculate from value
                if (initialData.days > 0) {
                    initialData.days.toString()
                } else {
                    (initialData.value / (24 * 60 * 60)).toString()
                }
            } else {
                "30"
            }
        )
    }
    var hoursValue by remember {
        mutableStateOf(
            if (initialData != null &&
                initialData.timelockType == MiniscriptTimelockType.RELATIVE &&
                initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK
            ) {
                // Use the new hours field if available, otherwise calculate from value
                if (initialData.hours > 0 || initialData.days > 0) {
                    initialData.hours.toString()
                } else {
                    ((initialData.value % (24 * 60 * 60)) / (60 * 60)).toString()
                }
            } else {
                "0"
            }
        )
    }
    var minutesValue by remember {
        mutableStateOf(
            if (initialData != null &&
                initialData.timelockType == MiniscriptTimelockType.RELATIVE &&
                initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK
            ) {
                // Use the new minutes field if available, otherwise calculate from value
                if (initialData.minutes > 0 || initialData.days > 0) {
                    initialData.minutes.toString()
                } else {
                    ((initialData.value % (60 * 60)) / 60).toString()
                }
            } else {
                "0"
            }
        )
    }



    var numericValue by remember {
        mutableStateOf(
            if (initialData != null &&
                !(initialData.timelockType == MiniscriptTimelockType.ABSOLUTE &&
                        initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK)
            ) {
                // For relative timelock or absolute block height, the value is already in the correct unit
                initialData.value.toString()
            } else {
                // Set default values based on timeUnit and timelockType
                when {
                    timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK -> "4320"
                    timeUnit == MiniscriptTimelockBased.TIME_LOCK && timelockType == MiniscriptTimelockType.RELATIVE -> "30"
                    else -> "30"
                }
            }
        )
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Update numericValue when timeUnit or timelockType changes (but not for initial data)
    LaunchedEffect(timeUnit, timelockType) {
        if (initialData == null ||
            (initialData.timelockType == MiniscriptTimelockType.ABSOLUTE &&
                    initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK)
        ) {
            numericValue = when {
                timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK -> "4320"
                timeUnit == MiniscriptTimelockBased.TIME_LOCK && timelockType == MiniscriptTimelockType.RELATIVE -> "30"
                else -> "30"
            }
        }
    }

    NunchukTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp) // Set maximum height to prevent excessive expansion
                .verticalScroll(scrollState)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "Edit timelock",
                style = NunchukTheme.typography.title
            )

            Text("Timelock type", style = NunchukTheme.typography.titleSmall)

            RadioOption(
                title = "Absolute time",
                description = "Unlock after a fixed point (specific date or block)",
                selected = timelockType == MiniscriptTimelockType.ABSOLUTE,
                onClick = { timelockType = MiniscriptTimelockType.ABSOLUTE }
            )

            RadioOption(
                title = "Relative time",
                description = "Unlocks after a set period from the time coins are received. Transfers within the same wallet will reset the timelock.",
                selected = timelockType == MiniscriptTimelockType.RELATIVE,
                onClick = { timelockType = MiniscriptTimelockType.RELATIVE }
            )

            HorizontalDivider()

            Text("Time unit", style = NunchukTheme.typography.titleSmall)

            RadioOption(
                title = "Timestamp",
                description = "Unlock after a specific time (Unix timestamp)",
                selected = timeUnit == MiniscriptTimelockBased.TIME_LOCK,
                onClick = { timeUnit = MiniscriptTimelockBased.TIME_LOCK }
            )

            RadioOption(
                title = "Block height",
                description = "Unlock after a specific block number",
                selected = timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK,
                onClick = { timeUnit = MiniscriptTimelockBased.HEIGHT_LOCK }
            )

            HorizontalDivider()

            DatePickerField(
                calendar = calendar,
                selectedDateText = selectedDateText,
                selectedTimeText = selectedTimeText,
                selectedTimeZone = selectedTimeZone,
                daysValue = daysValue,
                hoursValue = hoursValue,
                minutesValue = minutesValue,
                timeUnit = timeUnit,
                timelockType = timelockType,
                numericValue = numericValue,
                currentBlockHeight = currentBlockHeight,
                onNumericValueChange = { numericValue = it },
                onTimeZoneChange = { selectedTimeZone = it },
                onDaysValueChange = { daysValue = it },
                onHoursValueChange = { hoursValue = it },
                onMinutesValueChange = { minutesValue = it }
            )

            NcPrimaryDarkButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (timelockType == MiniscriptTimelockType.ABSOLUTE && timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK) {
                        val blockHeight = numericValue.toLongOrNull() ?: 0L
                        if (blockHeight < currentBlockHeight || blockHeight > 499999999) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    NcSnackbarVisuals(
                                        message = "Please enter a value between the current block height and 499999999",
                                        type = NcToastType.ERROR
                                    )
                                )
                            }
                            return@NcPrimaryDarkButton
                        }
                    } else if (timelockType == MiniscriptTimelockType.RELATIVE && timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
                        val days = daysValue.toLongOrNull() ?: 0L
                        val hours = hoursValue.toLongOrNull() ?: 0L
                        val minutes = minutesValue.toLongOrNull() ?: 0L
                        
                        if (days > 388) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    NcSnackbarVisuals(
                                        message = "Invalid value. Timelock must not exceed 388 days.",
                                        type = NcToastType.ERROR
                                    )
                                )
                            }
                            return@NcPrimaryDarkButton
                        }
                        
                        val totalSeconds = (days * 24 * 60 * 60) + (hours * 60 * 60) + (minutes * 60)
                        if (totalSeconds == 0L) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    NcSnackbarVisuals(
                                        message = "Please enter at least one time value.",
                                        type = NcToastType.ERROR
                                    )
                                )
                            }
                            return@NcPrimaryDarkButton
                        }
                    } else if (timelockType == MiniscriptTimelockType.RELATIVE && timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK) {
                        val blocks = numericValue.toLongOrNull() ?: 0L
                        if (blocks < 0 || blocks > 65534) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    NcSnackbarVisuals(
                                        message = "Invalid block height. Enter a value between 0 and 65,534 blocks.",
                                        type = NcToastType.ERROR
                                    )
                                )
                            }
                            return@NcPrimaryDarkButton
                        }
                    }

                    val value =
                        if (timelockType == MiniscriptTimelockType.ABSOLUTE && timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
                            // Convert to Unix timestamp considering the selected timezone
                            val selectedCal = Calendar.getInstance().apply {
                                timeInMillis = calendar.value.timeInMillis
                                timeZone = TimeZone.getTimeZone(selectedTimeZone.id)
                            }
                            selectedCal.timeInMillis / 1000 // Convert to Unix timestamp (seconds)
                        } else if (timelockType == MiniscriptTimelockType.RELATIVE && timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
                            // Convert days, hours, minutes to total seconds
                            val days = daysValue.toLongOrNull() ?: 0L
                            val hours = hoursValue.toLongOrNull() ?: 0L
                            val minutes = minutesValue.toLongOrNull() ?: 0L
                            (days * 24 * 60 * 60) + (hours * 60 * 60) + (minutes * 60)
                        } else {
                            numericValue.toLongOrNull() ?: 0L
                        }
                    
                    // Create TimelockData with appropriate fields based on type
                    val timelockData = when {
                        timelockType == MiniscriptTimelockType.ABSOLUTE && timeUnit == MiniscriptTimelockBased.TIME_LOCK -> {
                            TimelockData(
                                timelockType = timelockType,
                                timeUnit = timeUnit,
                                value = value,
                                timezoneId = selectedTimeZone.id,
                                timezoneCity = selectedTimeZone.city,
                                timezoneOffset = selectedTimeZone.offset
                            )
                        }
                        timelockType == MiniscriptTimelockType.RELATIVE && timeUnit == MiniscriptTimelockBased.TIME_LOCK -> {
                            TimelockData(
                                timelockType = timelockType,
                                timeUnit = timeUnit,
                                value = value,
                                days = daysValue.toLongOrNull() ?: 0L,
                                hours = hoursValue.toLongOrNull() ?: 0L,
                                minutes = minutesValue.toLongOrNull() ?: 0L
                            )
                        }
                        else -> {
                            TimelockData(
                                timelockType = timelockType,
                                timeUnit = timeUnit,
                                value = value
                            )
                        }
                    }
                    onSave(timelockData)
                }
            ) {
                Text(text = "Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    calendar: MutableState<Calendar>,
    selectedDateText: String,
    selectedTimeText: String = "",
    selectedTimeZone: TimeZoneDetail = TimeZoneDetail(),
    daysValue: String = "",
    hoursValue: String = "",
    minutesValue: String = "",
    timeUnit: MiniscriptTimelockBased,
    timelockType: MiniscriptTimelockType,
    numericValue: String,
    currentBlockHeight: Long = 0L,
    onNumericValueChange: (String) -> Unit,
    onTimeZoneChange: (TimeZoneDetail) -> Unit = {},
    onDaysValueChange: (String) -> Unit = {},
    onHoursValueChange: (String) -> Unit = {},
    onMinutesValueChange: (String) -> Unit = {}
) {
    var datePickerDialog by remember {
        mutableStateOf(false)
    }
    var timePickerDialog by remember {
        mutableStateOf(false)
    }
    var showTimeZoneDialog by remember {
        mutableStateOf(false)
    }


    val isTimestampCase =
        timelockType == MiniscriptTimelockType.ABSOLUTE && timeUnit == MiniscriptTimelockBased.TIME_LOCK

    val datePickerTextTitle = if (timelockType == MiniscriptTimelockType.ABSOLUTE) {
        if (timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
            "Unlock after a fixed date"
        } else {
            "Unlock after the target block number"
        }
    } else {
        "Unlock after time period"
    }

    val datePickerTextDesc =
        if (timelockType == MiniscriptTimelockType.ABSOLUTE && timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK) {
            val numberFormatter = DecimalFormat("#,###")
            "Current Bitcoin block height is ${numberFormatter.format(currentBlockHeight)}"
        } else {
            ""
        }

    val datePickerTitleHint = if (timelockType == MiniscriptTimelockType.RELATIVE) {
        if (timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
            "(days)"
        } else {
            "(blocks)"
        }
    } else {
        ""
    }

    if (timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
        Row {
            Text(
                text = if (timelockType == MiniscriptTimelockType.ABSOLUTE) {
                    "Unlock after a fixed date & time"
                } else {
                    "Unlock after time period"
                },
                style = NunchukTheme.typography.titleSmall,
            )
            Text(
                text = if (timelockType == MiniscriptTimelockType.ABSOLUTE) {
                    ""
                } else {
                    "(days, hours, minutes)"
                },
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    if (isTimestampCase) {
        // Timezone field
        NcTextField(
            modifier = Modifier.padding(top = 10.dp),
            title = "Time zone",
            value = if (selectedTimeZone.city.isNotEmpty()) {
                "${selectedTimeZone.city} (${selectedTimeZone.offset})"
            } else {
                "Select Time zone"
            },
            readOnly = true,
            enabled = false,
            onClick = {
                Timber.tag("miniscript-feature").d("Show timezone selection dialog")
                showTimeZoneDialog = true
            },
            rightContent = {
                Icon(
                    modifier = Modifier.padding(end = 12.dp).clickable {
                        Timber.tag("miniscript-feature").d("Show timezone selection dialog")
                        showTimeZoneDialog = true
                    },
                    painter = painterResource(id = com.nunchuk.android.core.R.drawable.ic_arrow_down),
                    contentDescription = ""
                )
            },
            onValueChange = {}
        )


        Row {
            // Date field
            NcTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                title = datePickerTextTitle,
                titleHint = datePickerTitleHint,
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
            Row(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

        }

        if (datePickerDialog) {
            NcDatePickerDialog(
                onDismissRequest = { datePickerDialog = false },
                onConfirm = { date ->
                    calendar.value = Calendar.getInstance().apply {
                        timeInMillis = date
                        // Preserve existing time when date changes
                        val currentCal = calendar.value
                        set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                    }
                    datePickerDialog = false
                },
                convertLocalToUtc = true,
                defaultDate = calendar.value.timeInMillis,
            )
        }

        if (timePickerDialog) {
            NcTimePickerDialog(
                onDismissRequest = { timePickerDialog = false },
                initialHour = calendar.value.get(Calendar.HOUR_OF_DAY),
                initialMinute = calendar.value.get(Calendar.MINUTE),
                onConfirm = { hour, minute ->
                    calendar.value = Calendar.getInstance().apply {
                        timeInMillis = calendar.value.timeInMillis
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }
                    timePickerDialog = false
                }
            )
        }


    } else {
        if (timelockType == MiniscriptTimelockType.RELATIVE && timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
            // Three input fields for relative time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NcTextField(
                    modifier = Modifier.weight(1f),
                    title = "Days",
                    value = daysValue,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            onDaysValueChange(value)
                        }
                    }
                )
                NcTextField(
                    modifier = Modifier.weight(1f),
                    title = "Hours",
                    value = hoursValue,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            onHoursValueChange(value)
                        }
                    }
                )
                NcTextField(
                    modifier = Modifier.weight(1f),
                    title = "Minutes",
                    value = minutesValue,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            onMinutesValueChange(value)
                        }
                    }
                )
            }
        } else {
            // Original single input field for other cases
            val numberFormatter = remember { DecimalFormat("#,###") }
            val displayValue = remember(numericValue, timeUnit) {
                if (timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK && numericValue.isNotEmpty()) {
                    try {
                        val number = numericValue.toLong()
                        numberFormatter.format(number)
                    } catch (e: NumberFormatException) {
                        numericValue
                    }
                } else {
                    numericValue
                }
            }

            NcTextField(
                title = datePickerTextTitle,
                titleHint = datePickerTitleHint,
                value = displayValue,
                onValueChange = { value ->
                    if (timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK) {
                        // For height lock, strip commas and only allow numeric input
                        val strippedValue = value.replace(",", "")
                        if (strippedValue.isEmpty() || strippedValue.all { it.isDigit() }) {
                            onNumericValueChange(strippedValue)
                        }
                    } else {
                        // For other cases, only allow numeric input
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            onNumericValueChange(value)
                        }
                    }
                },
                bottomContent = {
                    if (datePickerTextDesc.isNotEmpty()) {
                        Text(
                            text = datePickerTextDesc,
                            style = NunchukTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            )
        }
    }

    // Timezone selection dialog
    if (showTimeZoneDialog) {
        TimeZoneSelectionDialog(
            onDismissRequest = { showTimeZoneDialog = false },
            onTimeZoneSelected = { timeZone ->
                onTimeZoneChange(timeZone)
                showTimeZoneDialog = false
            }
        )
    }
}

@Composable
fun RadioOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = NunchukTheme.typography.body)
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = description,
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                )
            )
        }
        NcRadioButton(
            selected = selected,
            onClick = null, // Handled by parent Row
        )
    }
}



@PreviewLightDark
@Composable
fun EditTimelockBottomSheetPreview() {
    EditTimelockContent(
        snackbarHostState = remember { SnackbarHostState() }
    )
}
