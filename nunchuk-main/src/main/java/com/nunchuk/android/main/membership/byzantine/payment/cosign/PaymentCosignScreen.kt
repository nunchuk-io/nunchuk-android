package com.nunchuk.android.main.membership.byzantine.payment.cosign

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
fun PaymentCosignRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openPaymentNote: () -> Unit,
) {
    val state by recurringPaymentViewModel.config.collectAsStateWithLifecycle()
    PaymentCosignScreen(
        isCosign = state.isCosign,
        openPaymentNote = openPaymentNote,
        onIsCosignChange = recurringPaymentViewModel::onIsCosignChange,
    )
}

@Composable
private fun PaymentCosignScreen(
    isCosign: Boolean? = null,
    openPaymentNote : () -> Unit = {},
    onIsCosignChange: (Boolean) -> Unit = {},
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
                onClick = openPaymentNote,
                enabled = isCosign != null
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
                    text = stringResource(R.string.nc_please_specify_the_destination),
                    style = NunchukTheme.typography.body,
                )

                PaymentCosignOption(
                    modifier = Modifier.padding(top = 16.dp),
                    isSelected = isCosign == true,
                    title = stringResource(R.string.nc_allow_platform_key_to_co_sign),
                    desc = stringResource(R.string.nc_allow_platform_key_to_co_sign_desc),
                    onClick = {
                        onIsCosignChange(true)
                    }
                )

                PaymentCosignOption(
                    modifier = Modifier.padding(top = 16.dp),
                    isSelected = isCosign == false,
                    title = stringResource(R.string.nc_don_t_allow_platform_key_to_co_sign),
                    desc = stringResource(R.string.nc_don_t_allow_platform_key_to_co_sign_desc),
                    onClick = {
                        onIsCosignChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun PaymentCosignOption(
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
fun PaymentCosignScreenPreview() {
    PaymentCosignScreen()
}