package com.nunchuk.android.main.membership.byzantine.payment.note

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.MAX_NOTE_LENGTH
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

@Composable
fun PaymentNoteRoute(
    viewModel: RecurringPaymentViewModel,
    openSummaryScreen: () -> Unit,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    PaymentNoteScreen(
        note = config.note,
        onNoteChange = viewModel::onNoteChange,
        openSummaryScreen = openSummaryScreen,
    )
}

@Composable
fun PaymentNoteScreen(
    note: String = "",
    onNoteChange: (String) -> Unit = {},
    openSummaryScreen: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_add_recurring_payments),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = openSummaryScreen,
                        enabled = note.isNotEmpty(),
                    ) {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = openSummaryScreen,
                    ) {
                        Text(text = stringResource(R.string.nc_text_skip))
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_payment_note_title),
                    style = NunchukTheme.typography.body,
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxSize(),
                    title = stringResource(R.string.nc_transaction_note),
                    value = note,
                    onValueChange = { onNoteChange(it.take(MAX_NOTE_LENGTH)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    minLines = 3
                )
            }
        }
    }
}

@Preview
@Composable
fun PaymentNoteScreenPreview() {
    PaymentNoteScreen()
}