package com.nunchuk.android.main.membership.byzantine.payment.cosign

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val paymentCosignRoute = "payment_cosign"

fun NavGraphBuilder.addPaymentCosignGraph(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openPaymentNote: () -> Unit,
) {
    composable(paymentCosignRoute) {
        PaymentCosignRoute(
            recurringPaymentViewModel = recurringPaymentViewModel,
            openPaymentNote = openPaymentNote,
        )
    }
}

fun NavController.navigatePaymentCosign(
    navOptions: NavOptions? = null
) {
    navigate(paymentCosignRoute, navOptions)
}