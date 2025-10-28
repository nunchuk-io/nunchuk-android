package com.nunchuk.android.compose

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.nunchuk.android.core.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcDatePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
    dateValidator: (Long) -> Boolean = { it > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1) },
    defaultDate: Long? = null,
) {
    val calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = defaultDate ?: calendar.timeInMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return dateValidator(utcTimeMillis)
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onConfirm(it)
                }
            }) {
                Text(
                    text = stringResource(id = R.string.nc_ok),
                    color = MaterialTheme.colorScheme.textPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(id = R.string.nc_cancel),
                    color = MaterialTheme.colorScheme.textPrimary
                )
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = MaterialTheme.colorScheme.controlActivated,
                selectedDayContentColor = MaterialTheme.colorScheme.controlTextPrimary,
            ),
        )
    }
}