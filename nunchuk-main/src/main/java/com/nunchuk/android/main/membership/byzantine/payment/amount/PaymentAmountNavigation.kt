package com.nunchuk.android.main.membership.byzantine.payment.amount

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val amountPaymentRoute = "amount_payment"

fun NavGraphBuilder.addPaymentAmount(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openCalculateScreen: () -> Unit,
    openSelectAddressTypeScreen: () -> Unit,
) {
    composable(amountPaymentRoute) {
        PaymentAmountRoute(
            recurringPaymentViewModel = recurringPaymentViewModel,
            openCalculateScreen = openCalculateScreen,
            openSelectAddressTypeScreen = openSelectAddressTypeScreen,
        )
    }
}

fun NavController.navigateToPaymentAmount(navOptions: NavOptions? = null) {
    navigate(amountPaymentRoute, navOptions)
}