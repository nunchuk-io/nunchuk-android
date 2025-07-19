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
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.type.MiniscriptTimelockType
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale

data class TimelockData(
    val timelockType: MiniscriptTimelockType,
    val timeUnit: MiniscriptTimelockBased,
    val value: Long
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
                initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
                Calendar.getInstance().apply { timeInMillis = initialData.value }
            } else {
                Calendar.getInstance()
            }
        )
    }
    
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val selectedDateText by remember { derivedStateOf { dateFormat.format(calendar.value.time) } }
    
    var numericValue by remember { 
        mutableStateOf(
            if (initialData != null && 
                !(initialData.timelockType == MiniscriptTimelockType.ABSOLUTE && 
                  initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK)) {
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
             initialData.timeUnit == MiniscriptTimelockBased.TIME_LOCK)) {
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
                timeUnit = timeUnit,
                timelockType = timelockType,
                numericValue = numericValue,
                currentBlockHeight = currentBlockHeight,
                onNumericValueChange = { numericValue = it }
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
                        val days = numericValue.toLongOrNull() ?: 0L
                        if (days < 0 || days > 388) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    NcSnackbarVisuals(
                                        message = "Invalid timestamp. Enter a value between 0 and 388 days.",
                                        type = NcToastType.ERROR
                                    )
                                )
                            }
                            return@NcPrimaryDarkButton
                        }
                    } else if (timelockType == MiniscriptTimelockType.RELATIVE && timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK) {
                        val blocks = numericValue.toLongOrNull() ?: 0L
                        if (blocks < 0 || blocks > 65535) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    NcSnackbarVisuals(
                                        message = "Invalid block height. Enter a value between 0 and 65,535 blocks.",
                                        type = NcToastType.ERROR
                                    )
                                )
                            }
                            return@NcPrimaryDarkButton
                        }
                    } else if (timelockType == MiniscriptTimelockType.ABSOLUTE && timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
                        val today = Calendar.getInstance()
                        val selectedDate = calendar.value
                        val maxYear = Calendar.getInstance().apply { set(Calendar.YEAR, 11516) }

                        if (selectedDate.before(today) || selectedDate.after(maxYear)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    NcSnackbarVisuals(
                                        message = "Please select a date between today and the year 11516",
                                        type = NcToastType.ERROR
                                    )
                                )
                            }
                            return@NcPrimaryDarkButton
                        }
                    }

                    val value =
                        if (timelockType == MiniscriptTimelockType.ABSOLUTE && timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
                            calendar.value.timeInMillis
                        } else {
                            numericValue.toLongOrNull() ?: 0L
                        }
                    onSave(TimelockData(timelockType, timeUnit, value))
                }
            ) {
                Text(text = "Save")
            }
        }
    }
}

@Composable
fun DatePickerField(
    calendar: MutableState<Calendar>,
    selectedDateText: String,
    timeUnit: MiniscriptTimelockBased,
    timelockType: MiniscriptTimelockType,
    numericValue: String,
    currentBlockHeight: Long = 0L,
    onNumericValueChange: (String) -> Unit
) {
    var datePickerDialog by remember {
        mutableStateOf(false)
    }

    val isTimestampCase = timelockType == MiniscriptTimelockType.ABSOLUTE && timeUnit == MiniscriptTimelockBased.TIME_LOCK

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

    if (isTimestampCase) {
        NcTextField(
            title = datePickerTextTitle,
            titleHint = datePickerTitleHint,
            value = selectedDateText,
            readOnly = true,
            onClick = {
                datePickerDialog = true
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
            }
        ) {

        }

        if (datePickerDialog) {
            NcDatePickerDialog(
                onDismissRequest = { datePickerDialog = false },
                onConfirm = { date ->
                    calendar.value = Calendar.getInstance().apply { timeInMillis = date }
                    datePickerDialog = false
                }
            )
        }
    } else {
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
