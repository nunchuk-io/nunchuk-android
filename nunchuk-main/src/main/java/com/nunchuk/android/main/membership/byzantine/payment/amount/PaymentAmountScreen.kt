package com.nunchuk.android.main.membership.byzantine.payment.amount

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcNumberInputField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSelectableBottomSheet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentAmountScreen(
    unit: SpendingCurrencyUnit = SpendingCurrencyUnit.CURRENCY_UNIT,
    amount: String = "",
    onAmountChange: (String) -> Unit = {},
    onUnitChange: (SpendingCurrencyUnit) -> Unit = {},
    openCalculateScreen: () -> Unit = {},
) {
    val context = LocalContext.current
    var showSelectUnitSheet by remember {
        mutableStateOf(false)
    }
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
                    NcNumberInputField(
                        modifier = Modifier
                            .padding(top = 16.dp, end = 16.dp)
                            .weight(2f),
                        title = stringResource(R.string.nc_fixed_amount),
                        value = amount,
                        onValueChange = onAmountChange,
                    )

                    Row(
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = Color(0xFFDEDEDE),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable { showSelectUnitSheet = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
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

            if (showSelectUnitSheet) {
                NcSelectableBottomSheet(
                    options = SpendingCurrencyUnit.values().map { it.toLabel(context) },
                    selectedPos = unit.ordinal,
                    onSelected = {
                        onUnitChange(SpendingCurrencyUnit.values()[it])
                        showSelectUnitSheet = false
                    },
                    onDismiss = { showSelectUnitSheet = false },
                )
            }
        }
    }
}

@Preview
@Composable
fun PaymentAmountScreenPreview() {
    PaymentAmountScreen()
}