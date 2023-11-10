package com.nunchuk.android.main.membership.byzantine.payment.feerate

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val paymentFeeRateRoute = "payment_fee_rate"
fun NavGraphBuilder.addPaymentFeeRateScreen(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openPaymentCosignScreen: () -> Unit,
) {
    composable(paymentFeeRateRoute) {
        PaymentFeeRateRoute(
            recurringPaymentViewModel = recurringPaymentViewModel,
            openPaymentCosignScreen = openPaymentCosignScreen,
        )
    }
}

fun NavController.navigateToPaymentFeeRate(
    navOptions: NavOptions? = null
) {
    navigate(paymentFeeRateRoute, navOptions)
}