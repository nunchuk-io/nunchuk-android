package com.nunchuk.android.main.membership.byzantine.payment.paymentpercentage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

@Composable
fun PaymentPercentageRoute(
    viewModel: RecurringPaymentViewModel,
    openSelectAddressTypeScreen: () -> Unit,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    PaymentPercentageScreen(
        openSelectAddressTypeScreen = openSelectAddressTypeScreen,
        calculatePercentageJustInTime = config.calculatePercentageJustInTime,
        onCalculatePercentageJustInTimeChange = viewModel::onCalculatePercentageJustInTimeChange,
    )
}

@Composable
private fun PaymentPercentageScreen(
    openSelectAddressTypeScreen: () -> Unit = {},
    calculatePercentageJustInTime: Boolean? = null,
    onCalculatePercentageJustInTimeChange: (Boolean) -> Unit = {},
) {
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
                onClick = openSelectAddressTypeScreen,
                enabled = calculatePercentageJustInTime != null,
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
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_please_name_the_payment),
                    style = NunchukTheme.typography.body,
                )

                PercentageOption(
                    modifier = Modifier.padding(top = 16.dp),
                    isSelected = calculatePercentageJustInTime == false,
                    title = stringResource(R.string.nc_running_average),
                    desc = stringResource(R.string.nc_running_average_desc),
                    onClick = {
                        onCalculatePercentageJustInTimeChange(false)
                    }
                )

                PercentageOption(
                    modifier = Modifier.padding(top = 16.dp),
                    isSelected = calculatePercentageJustInTime == true,
                    title = stringResource(R.string.nc_just_in_time),
                    desc = stringResource(R.string.nc_calculate_percentage_just_in_time_desc),
                    onClick = {
                        onCalculatePercentageJustInTimeChange(true)
                    }
                )
            }
        }
    }
}

@Composable
private fun PercentageOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: String,
    desc: String,
    onClick: () -> Unit = {},
) {
    NcRadioOption(modifier = modifier.fillMaxWidth(), isSelected = isSelected, onClick = onClick) {
        Text(text = title, style = NunchukTheme.typography.title)
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = desc,
            style = NunchukTheme.typography.body
        )
    }
}

@Preview
@Composable
fun PaymentPercentageScreenPreview() {
    PaymentPercentageScreen()
}