package com.nunchuk.android.main.membership.byzantine.payment.selectmethod

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import com.nunchuk.android.share.ColdcardAction

const val paymentSelectAddressTypeRoute = "payment_select_address_type"

fun NavGraphBuilder.addPaymentSelectAddressType(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openWhiteListAddressScreen: () -> Unit,
    openScanQRCodeScreen: () -> Unit,
    openBsmsScreen: () -> Unit,
    openScanMk4: (ColdcardAction) -> Unit,
    openSellectWallet: () -> Unit,
) {
    composable(paymentSelectAddressTypeRoute) {
        PaymentSelectAddressTypeRoute(
            recurringPaymentViewModel = recurringPaymentViewModel,
            openWhiteListAddressScreen = openWhiteListAddressScreen,
            openScanQRCodeScreen = openScanQRCodeScreen,
            openBsmsScreen = openBsmsScreen,
            openScanMk4 = openScanMk4,
            openSellectWallet = openSellectWallet
        )
    }
}

fun NavController.navigateToPaymentSelectAddressType(navOptions: NavOptions? = null) {
    navigate(paymentSelectAddressTypeRoute, navOptions)
}