package com.nunchuk.android.main.membership.byzantine.payment.name

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val namePaymentRoute = "name_payment"
fun NavGraphBuilder.addPaymentName(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openPaymentAmountScreen: () -> Unit,
) {
    composable(namePaymentRoute) {
        PaymentNameRoute(
            recurringPaymentViewModel,
            openPaymentAmountScreen = openPaymentAmountScreen,
        )
    }
}

fun NavController.navigateToPaymentName(
    navOptions: NavOptions? = null,
) {
    navigate(namePaymentRoute, navOptions)
}