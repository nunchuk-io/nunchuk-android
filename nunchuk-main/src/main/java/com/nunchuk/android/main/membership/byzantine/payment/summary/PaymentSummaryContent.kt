package com.nunchuk.android.main.membership.byzantine.payment.summary

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcExpandableText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.AddressWithQrView
import com.nunchuk.android.main.membership.byzantine.payment.feerate.toTitle
import com.nunchuk.android.main.membership.byzantine.payment.toResId
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.payment.PaymentCalculationMethod
import com.nunchuk.android.model.payment.PaymentDestinationType
import com.nunchuk.android.model.payment.PaymentFrequency
import com.nunchuk.android.utils.simpleGlobalDateFormat
import java.util.Date

@Composable
fun PaymentSummaryContent(
    modifier: Modifier = Modifier,
    isCosign: Boolean?,
    name: String,
    amount: String,
    frequency: PaymentFrequency,
    destinationType: PaymentDestinationType,
    calculationMethod: PaymentCalculationMethod?,
    startDate: Long,
    noEndDate: Boolean,
    endDate: Long,
    feeRate: FeeRate,
    addresses: List<String>,
    note: String,
    currency: String?,
    useAmount: Boolean,
    bsms: String?,
    openQRDetailScreen: (address: String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (isCosign == true) {
            NcHintMessage(messages = listOf(ClickAbleText(stringResource(R.string.nc_payment_cosign_enable_warning))))
        }
        NcTextField(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            title = stringResource(id = R.string.nc_payment_name),
            titleStyle = NunchukTheme.typography.title,
            value = name,
            onValueChange = {},
            enabled = false,
            disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
        )

        NcTextField(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            title = stringResource(id = R.string.nc_amount),
            titleStyle = NunchukTheme.typography.title,
            value = "$amount ${if (useAmount) currency.orEmpty() else "% of wallet balance (*)"}",
            onValueChange = {},
            enabled = false,
            disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
        )

        calculationMethod?.let {
            Text(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                text = if (calculationMethod == PaymentCalculationMethod.RUNNING_AVERAGE)
                    stringResource(R.string.nc_run_in_average_desc)
                else stringResource(R.string.nc_just_in_time_desc),
                style = NunchukTheme.typography.body,
            )
        }

        NcTextField(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            title = stringResource(id = R.string.nc_repeat),
            titleStyle = NunchukTheme.typography.title,
            value = stringResource(id = frequency.toResId()),
            onValueChange = {},
            enabled = false,
            disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
        )

        Row {
            NcTextField(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .weight(1f),
                title = stringResource(id = R.string.nc_start_date),
                titleStyle = NunchukTheme.typography.title,
                value = Date(startDate).simpleGlobalDateFormat(),
                onValueChange = {},
                enabled = false,
                disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
            )
            Spacer(modifier = Modifier.width(16.dp))
            NcTextField(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .weight(1f),
                title = stringResource(id = R.string.nc_end_date),
                titleStyle = NunchukTheme.typography.title,
                value = if (noEndDate) stringResource(id = R.string.nc_no_end_date)
                else Date(endDate).simpleGlobalDateFormat(),
                onValueChange = {},
                enabled = false,
                disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
            )
        }

        NcTextField(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.nc_fee_rate),
            titleStyle = NunchukTheme.typography.title,
            value = stringResource(id = feeRate.toTitle()),
            onValueChange = {},
            enabled = false,
            disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
        )

        if (isCosign != null) {
            NcTextField(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                title = stringResource(R.string.nc_allow_platform_key_to_co_sign),
                titleStyle = NunchukTheme.typography.title,
                value = if (isCosign) stringResource(id = R.string.nc_text_yes)
                else stringResource(id = R.string.nc_text_no),
                onValueChange = {},
                enabled = false,
                disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
            )
        }

        if (destinationType == PaymentDestinationType.DESTINATION_WALLET) {
            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = stringResource(R.string.nc_destination_wallet),
                style = NunchukTheme.typography.title
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.nc_sent_to_first_available_address),
                style = NunchukTheme.typography.body
            )
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(R.string.nc_first_address_of_wallet),
                style = NunchukTheme.typography.titleSmall
            )
            AddressWithQrView(address = addresses.first(), openQRDetailScreen)

            if (!bsms.isNullOrEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_configuration_details),
                    style = NunchukTheme.typography.titleSmall,
                )

                NcExpandableText(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = NunchukTheme.shape.medium
                        )
                        .padding(16.dp),
                    text = bsms,
                    style = NunchukTheme.typography.body,
                    maxLines = 6
                )
            }
        } else {
            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = stringResource(R.string.nc_destination_whitelisted_addresses),
                style = NunchukTheme.typography.title,
            )
            addresses.forEach { address ->
                AddressWithQrView(address, openQRDetailScreen = openQRDetailScreen)
            }
        }

        NcTextField(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.nc_transaction_note),
            titleStyle = NunchukTheme.typography.title,
            value = note.ifEmpty { stringResource(R.string.nc_no_transaction_note) },
            onValueChange = {},
            enabled = false,
            disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
        )
    }
}