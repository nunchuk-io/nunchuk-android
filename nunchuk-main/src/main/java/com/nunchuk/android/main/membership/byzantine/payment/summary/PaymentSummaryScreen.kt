package com.nunchuk.android.main.membership.byzantine.payment.summary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentConfig
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import com.nunchuk.android.main.membership.byzantine.payment.feerate.toTitle
import com.nunchuk.android.main.membership.byzantine.payment.toResId
import com.nunchuk.android.model.payment.PaymentFrequency
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.utils.simpleGlobalDateFormat
import java.util.Date

@Composable
fun PaymentSummaryRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
) {
    val state by recurringPaymentViewModel.config.collectAsStateWithLifecycle()
    PaymentSummaryScreen(state = state)
}

@Composable
fun PaymentSummaryScreen(state: RecurringPaymentConfig = RecurringPaymentConfig()) {
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_add_recurring_payments),
                textStyle = NunchukTheme.typography.titleLarge,
            )
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = {},
            ) {
                Text(text = stringResource(R.string.nc_text_continue))
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (state.isCosign == true) {
                    NcHintMessage(messages = listOf(ClickAbleText(stringResource(R.string.nc_payment_cosign_enable_warning))))
                }
                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(id = R.string.nc_payment_name),
                    value = state.name,
                    onValueChange = {},
                    enabled = false,
                    disableBackgroundColor = MaterialTheme.colorScheme.whisper,
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(id = R.string.nc_amount),
                    value = state.amount,
                    onValueChange = {},
                    enabled = false,
                    disableBackgroundColor = MaterialTheme.colorScheme.whisper,
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(id = R.string.nc_repeat),
                    value = state.frequency?.toResId()?.let { stringResource(id = it) }.orEmpty(),
                    onValueChange = {},
                    enabled = false,
                    disableBackgroundColor = MaterialTheme.colorScheme.whisper,
                )

                Row {
                    NcTextField(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .weight(1f),
                        title = stringResource(id = R.string.nc_start_date),
                        value = Date(state.startDate).simpleGlobalDateFormat(),
                        onValueChange = {},
                        enabled = false,
                        disableBackgroundColor = MaterialTheme.colorScheme.whisper,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    NcTextField(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .weight(1f),
                        title = stringResource(id = R.string.nc_end_date),
                        value = if (state.noEndDate) stringResource(id = R.string.nc_no_end_date)
                        else Date(state.endDate).simpleGlobalDateFormat(),
                        onValueChange = {},
                        enabled = false,
                        disableBackgroundColor = MaterialTheme.colorScheme.whisper,
                    )
                }

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.nc_fee_rate),
                    value = stringResource(id = state.feeRate.toTitle()),
                    onValueChange = {},
                    enabled = false,
                    disableBackgroundColor = MaterialTheme.colorScheme.whisper,
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.nc_allow_platform_key_to_co_sign),
                    value = if (state.isCosign == true) stringResource(id = R.string.nc_text_yes)
                    else stringResource(id = R.string.nc_text_no),
                    onValueChange = {},
                    enabled = false,
                    disableBackgroundColor = MaterialTheme.colorScheme.whisper,
                )

                if (state.addresses.isNotEmpty()) {

                } else {

                }

                if (state.note.isNotEmpty()) {
                    NcTextField(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        title = stringResource(R.string.nc_note),
                        value = state.note,
                        onValueChange = {},
                        enabled = false,
                        disableBackgroundColor = MaterialTheme.colorScheme.whisper,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PaymentSummaryScreenPreview() {
    PaymentSummaryScreen(
        RecurringPaymentConfig(
            name = "Consultant fee",
            note = "Lorem ipsum dolore",
            feeRate = FeeRate.PRIORITY,
            frequency = PaymentFrequency.DAILY,
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis() + 1000000L,
            noEndDate = false,
            amount = "0.0001",
        )
    )
}
