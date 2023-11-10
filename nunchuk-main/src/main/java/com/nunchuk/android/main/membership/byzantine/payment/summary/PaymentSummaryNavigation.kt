package com.nunchuk.android.main.membership.byzantine.payment.summary

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val paymentSummaryRoute = "payment_summary"
fun NavGraphBuilder.addPaymentSummary(
    recurringPaymentViewModel: RecurringPaymentViewModel
) {
    composable(paymentSummaryRoute) {
        PaymentSummaryRoute(recurringPaymentViewModel = recurringPaymentViewModel)
    }
}

fun NavController.navigateToPaymentSummary(
    navOptions: NavOptions? = null
) {
    navigate(paymentSummaryRoute, navOptions)
}