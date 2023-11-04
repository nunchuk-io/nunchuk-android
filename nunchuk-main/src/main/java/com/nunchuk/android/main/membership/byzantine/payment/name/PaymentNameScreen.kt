package com.nunchuk.android.main.membership.byzantine.payment.name

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

@Composable
fun PaymentNameRoute(
    viewModel: RecurringPaymentViewModel,
    openPaymentAmountScreen: () -> Unit,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    PaymentNameScreen(
        name = config.name,
        onNameChange = viewModel::onNameChange,
        openPaymentAmountScreen = openPaymentAmountScreen,
    )
}

@Composable
fun PaymentNameScreen(
    name: String = "",
    onNameChange: (String) -> Unit = {},
    openPaymentAmountScreen: () -> Unit = {},
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
                onClick = openPaymentAmountScreen,
                enabled = name.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.nc_text_continue))
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_please_name_the_payment),
                    style = NunchukTheme.typography.body,
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxSize(),
                    title = stringResource(R.string.nc_payment_name),
                    value = name,
                    onValueChange = onNameChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    maxLines = 1
                )
            }
        }
    }
}

@Preview
@Composable
fun PaymentNameScreenPreview() {
    PaymentNameScreen()
}