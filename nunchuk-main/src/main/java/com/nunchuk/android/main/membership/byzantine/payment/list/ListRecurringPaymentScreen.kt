package com.nunchuk.android.main.membership.byzantine.payment.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

@Composable
fun ListRecurringPaymentRoute(
    viewModel: ListRecurringPaymentViewModel = hiltViewModel(),
    onOpenAddRecurringPayment: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ListRecurringPaymentScreen(
        uiState = state,
        onOpenAddRecurringPayment = onOpenAddRecurringPayment,
    )
}

@Composable
fun ListRecurringPaymentScreen(
    uiState: ListRecurringPaymentUiState = ListRecurringPaymentUiState(),
    onOpenAddRecurringPayment: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_recurring_payments),
                    isBack = false,
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(), onClick = onOpenAddRecurringPayment
                ) {
                    Text(text = stringResource(R.string.nc_add_recurring_payments))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                if (uiState.payments.isEmpty()) {
                    NcCircleImage(
                        resId = R.drawable.ic_pending_transaction,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(CenterHorizontally),
                        size = 96.dp,
                        iconSize = 60.dp,
                    )

                    Text(
                        text = stringResource(R.string.nc_recurring_payments_intro_desc),
                        modifier = Modifier.padding(16.dp),
                        style = NunchukTheme.typography.body,
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.payments) { payment ->
                            RecurringPaymentItemView(recurringPayment = payment)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListRecurringPaymentScreenPreview() {
    ListRecurringPaymentScreen()
}