package com.nunchuk.android.main.membership.byzantine.payment.frequent

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val addPaymentFrequencyRoute = "add_payment_frequency"
fun NavGraphBuilder.addPaymentFrequency(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openPaymentFeeRateScreen: () -> Unit,
) {
    composable(addPaymentFrequencyRoute) {
        PaymentFrequentRoute(
            viewModel = recurringPaymentViewModel,
            openPaymentFeeRateScreen = openPaymentFeeRateScreen,
        )
    }
}

fun NavController.navigateToPaymentFrequency(
    navOptions: NavOptions? = null,
) {
    navigate(addPaymentFrequencyRoute, navOptions)
}