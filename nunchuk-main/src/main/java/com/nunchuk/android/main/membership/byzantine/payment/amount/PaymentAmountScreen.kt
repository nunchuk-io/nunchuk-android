package com.nunchuk.android.main.membership.byzantine.payment.amount

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import com.nunchuk.android.main.membership.key.server.limit.toLabel
import com.nunchuk.android.model.SpendingCurrencyUnit

@Composable
fun PaymentAmountRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openCalculateScreen: () -> Unit,
) {
    val config by recurringPaymentViewModel.config.collectAsStateWithLifecycle()
    PaymentAmountScreen(
        openCalculateScreen = openCalculateScreen,
        amount = config.amount,
        unit = config.unit,
        onAmountChange = recurringPaymentViewModel::onAmountChange,
        onUnitChange = recurringPaymentViewModel::onUnitChange,
    )
}

@Composable
fun PaymentAmountScreen(
    unit: SpendingCurrencyUnit = SpendingCurrencyUnit.CURRENCY_UNIT,
    amount: String = "",
    onAmountChange: (String) -> Unit = {},
    onUnitChange: (SpendingCurrencyUnit) -> Unit = {},
    openCalculateScreen: () -> Unit = {},
) {
    val context = LocalContext.current
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
                onClick = openCalculateScreen,
                enabled = amount.isNotEmpty()
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
                    text = stringResource(R.string.nc_please_enter_the_amount_for_the_payment),
                    style = NunchukTheme.typography.body,
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    NcTextField(
                        modifier = Modifier
                            .padding(top = 16.dp, end = 16.dp)
                            .weight(1f),
                        title = stringResource(R.string.nc_payment_name),
                        value = amount,
                        onValueChange = onAmountChange
                    )

                    Row(
                        modifier = Modifier
                            .height(50.dp)
                            .border(
                                width = 1.dp,
                                color = Color(0xFFDEDEDE),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable { onUnitChange(unit) }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(end = 16.dp),
                            text = unit.toLabel(context),
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PaymentAmountScreenPreview() {
    PaymentAmountScreen()
}