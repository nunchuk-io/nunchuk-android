package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nunchuk.android.core.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    initialMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = "Select Time",
                    style = MaterialTheme.typography.labelMedium
                )
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surface,
                        selectorColor = MaterialTheme.colorScheme.controlActivated,
                        containerColor = MaterialTheme.colorScheme.surface,
                        periodSelectorBorderColor = MaterialTheme.colorScheme.controlActivated,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.controlActivated,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.controlTextPrimary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.controlActivated,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.controlTextPrimary,
                    ),
                )
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = stringResource(id = R.string.nc_cancel),
                            color = MaterialTheme.colorScheme.textPrimary
                        )
                    }
                    TextButton(onClick = {
                        onConfirm(timePickerState.hour, timePickerState.minute)
                    }) {
                        Text(
                            text = stringResource(id = R.string.nc_ok),
                            color = MaterialTheme.colorScheme.textPrimary
                        )
                    }
                }
            }
        }
    }
}