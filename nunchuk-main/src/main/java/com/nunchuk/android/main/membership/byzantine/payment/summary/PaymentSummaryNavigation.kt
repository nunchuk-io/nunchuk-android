package com.nunchuk.android.main.membership.byzantine.payment.summary

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import com.nunchuk.android.model.byzantine.DummyTransactionPayload

const val paymentSummaryRoute = "payment_summary"
fun NavGraphBuilder.addPaymentSummary(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openDummyTransactionScreen: (DummyTransactionPayload) -> Unit,
    openQRDetailScreen: (address: String) -> Unit,
) {
    composable(paymentSummaryRoute) {
        PaymentSummaryRoute(
            recurringPaymentViewModel = recurringPaymentViewModel,
            openDummyTransactionScreen = openDummyTransactionScreen,
            openQRDetailScreen = openQRDetailScreen
        )
    }
}

fun NavController.navigateToPaymentSummary(
    navOptions: NavOptions? = null,
) {
    navigate(paymentSummaryRoute, navOptions)
}