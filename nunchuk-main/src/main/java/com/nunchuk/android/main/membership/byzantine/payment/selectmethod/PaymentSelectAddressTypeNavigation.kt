package com.nunchuk.android.main.membership.byzantine.payment.selectmethod

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val paymentSelectAddressTypeRoute = "payment_select_address_type"

fun NavGraphBuilder.addPaymentSelectAddressType(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openWhiteListAddressScreen: () -> Unit,
    openScanQRCodeScreen: () -> Unit,
    openBsmsScreen: () -> Unit,
) {
    composable(paymentSelectAddressTypeRoute) {
        PaymentSelectAddressTypeRoute(
            recurringPaymentViewModel = recurringPaymentViewModel,
            openWhiteListAddressScreen = openWhiteListAddressScreen,
            openScanQRCodeScreen = openScanQRCodeScreen,
            openBsmsScreen = openBsmsScreen,
        )
    }
}

fun NavController.navigateToPaymentSelectAddressType(navOptions: NavOptions? = null) {
    navigate(paymentSelectAddressTypeRoute, navOptions)
}