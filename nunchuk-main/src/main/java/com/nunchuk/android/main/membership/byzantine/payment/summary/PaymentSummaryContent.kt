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
            value = "$amount ${if (useAmount) currency.orEmpty() else "%"}",
            onValueChange = {},
            enabled = false,
            disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
        )

        NcTextField(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            title = stringResource(id = R.string.nc_repeat),
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
                value = if (isCosign) stringResource(id = R.string.nc_text_yes)
                else stringResource(id = R.string.nc_text_no),
                onValueChange = {},
                enabled = false,
                disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
            )
        }

        if (!bsms.isNullOrEmpty()) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(R.string.nc_first_address_of_wallet),
                style = NunchukTheme.typography.title
            )
            AddressWithQrView(address = addresses.first(), openQRDetailScreen)

            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(R.string.nc_configuration_details),
                style = NunchukTheme.typography.title,
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

        if (note.isNotEmpty()) {
            NcTextField(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                title = stringResource(R.string.nc_note),
                value = note,
                onValueChange = {},
                enabled = false,
                disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
            )
        }
    }
}