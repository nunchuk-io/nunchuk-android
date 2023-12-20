package com.nunchuk.android.main.membership.byzantine.payment.note

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val paymentNoteRoute = "payment_note"
fun NavGraphBuilder.addPaymentNoteScreen(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openSummaryScreen: () -> Unit,
) {
    composable(paymentNoteRoute) {
        PaymentNoteRoute(
            viewModel = recurringPaymentViewModel,
            openSummaryScreen = openSummaryScreen,
        )
    }
}

fun NavController.navigateToPaymentNote(
    navOptions: NavOptions? = null
) {
    navigate(paymentNoteRoute, navOptions)
}