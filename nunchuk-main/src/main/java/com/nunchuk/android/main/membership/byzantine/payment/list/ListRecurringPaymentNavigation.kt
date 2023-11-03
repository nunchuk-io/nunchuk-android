package com.nunchuk.android.main.membership.byzantine.payment.list

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val recurringPaymentRoute = "recurring_payment"

fun NavGraphBuilder.recurringPaymentsList(
    onOpenAddRecurringPayment: () -> Unit,
) {
    composable(recurringPaymentRoute) {
        ListRecurringPaymentRoute(
            onOpenAddRecurringPayment = onOpenAddRecurringPayment,
        )
    }
}

fun NavController.navigateToListRecurringPayment(
    navOptions: NavOptions? = null,
) {
    navigate(recurringPaymentRoute, navOptions)
}
