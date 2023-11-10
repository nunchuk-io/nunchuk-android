package com.nunchuk.android.compose

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.core.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcDatePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onConfirm(it)
                }
            }) {
                Text(text = stringResource(id = R.string.nc_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.nc_cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
}