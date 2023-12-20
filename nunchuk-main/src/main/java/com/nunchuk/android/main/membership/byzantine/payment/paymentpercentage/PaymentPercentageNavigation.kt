package com.nunchuk.android.main.membership.byzantine.payment.paymentpercentage

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val paymentPercentageCalculationRoute = "payment_percentage"
fun NavGraphBuilder.addPaymentPercentageCalculation(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openSelectAddressTypeScreen: () -> Unit,
) {
    composable(paymentPercentageCalculationRoute) {
        PaymentPercentageRoute(
            openSelectAddressTypeScreen = openSelectAddressTypeScreen,
            viewModel = recurringPaymentViewModel,
        )
    }
}

fun NavController.navigateToPaymentPercentageCalculation(navOptions: NavOptions? = null) {
    navigate(paymentPercentageCalculationRoute, navOptions)
}