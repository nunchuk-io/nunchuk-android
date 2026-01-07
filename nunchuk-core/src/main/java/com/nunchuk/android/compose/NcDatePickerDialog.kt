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
    convertLocalToUtc: Boolean = false,
) {
    val calendar = Calendar.getInstance()
    val initialDateMillis = defaultDate?.let { localMillis ->
        if (convertLocalToUtc) {
            val localCalendar = Calendar.getInstance().apply {
                timeInMillis = localMillis
            }
            val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, localCalendar.get(Calendar.YEAR))
                set(Calendar.MONTH, localCalendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            utcCalendar.timeInMillis
        } else {
            localMillis
        }
    } ?: calendar.timeInMillis
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
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