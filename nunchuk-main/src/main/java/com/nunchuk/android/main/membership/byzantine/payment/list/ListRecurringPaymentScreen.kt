package com.nunchuk.android.main.membership.byzantine.payment.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isMasterOrAdmin


@Composable
fun ListRecurringPaymentRoute(
    viewModel: ListRecurringPaymentViewModel = hiltViewModel(),
    onOpenAddRecurringPayment: () -> Unit = {},
    onOpenRecurringPaymentDetail: (String) -> Unit = {},
    myRole: AssistedWalletRole = AssistedWalletRole.NONE,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ListRecurringPaymentScreen(
        uiState = state,
        myRole = myRole,
        onOpenAddRecurringPayment = onOpenAddRecurringPayment,
        onOpenRecurringPaymentDetail = onOpenRecurringPaymentDetail,
        sortRecurringpaymentItem = viewModel::sort,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListRecurringPaymentScreen(
    uiState: ListRecurringPaymentUiState = ListRecurringPaymentUiState(),
    myRole: AssistedWalletRole = AssistedWalletRole.NONE,
    onOpenAddRecurringPayment: () -> Unit = {},
    onOpenRecurringPaymentDetail: (String) -> Unit = {},
    sortRecurringpaymentItem: (sortBy: SortBy) -> Unit = {},
) {
    var showSortOptionBottomSheet by remember {
        mutableStateOf(false)
    }
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_recurring_payments),
                    isBack = false,
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Box {
                            IconButton(onClick = { showSortOptionBottomSheet = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sort_dark),
                                    contentDescription = " Sort Dark"
                                )
                            }
                            if (uiState.sortBy != SortBy.NONE) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(12.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.error,
                                            shape = CircleShape
                                        )
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (myRole.isMasterOrAdmin) {
                    if (uiState.payments.isEmpty()) {
                        NcPrimaryDarkButton(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            onClick = onOpenAddRecurringPayment
                        ) {
                            Text(text = stringResource(R.string.nc_add_recurring_payments))
                        }
                    } else {
                        NcOutlineButton(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            onClick = onOpenAddRecurringPayment
                        ) {
                            Text(text = stringResource(R.string.nc_add_recurring_payments))
                        }
                    }
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
                        resId = R.drawable.ic_new_pending_transaction,
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
                            RecurringPaymentItemView(
                                recurringPayment = payment,
                                onClick = onOpenRecurringPaymentDetail
                            )
                        }
                    }
                }
            }
            if (showSortOptionBottomSheet) {
                SortOptionButtonSheet(
                    onDismiss = { showSortOptionBottomSheet = false },
                    sortRecurringpaymentItem = {
                        sortRecurringpaymentItem(it)
                        showSortOptionBottomSheet = false
                    },
                    initValue = uiState.sortBy
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListRecurringPaymentScreenPreview() {
    ListRecurringPaymentScreen(
        uiState = ListRecurringPaymentUiState(
            sortBy = SortBy.AZ,
        )
    )
}