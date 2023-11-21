package com.nunchuk.android.main.membership.byzantine.payment.address.wallet

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val paymentWalletAddressRoute = "payment_wallet_address"

fun NavGraphBuilder.addPaymentWalletAddress(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    openPaymentFrequencyScreen: () -> Unit,
    onOpenQrDetailScreen: (address: String) -> Unit,
) {
    composable(paymentWalletAddressRoute) {
        PaymentWalletAddressRoute(
            viewModel = recurringPaymentViewModel,
            openPaymentFrequencyScreen = openPaymentFrequencyScreen,
            onOpenQrDetailScreen = onOpenQrDetailScreen,
        )
    }
}

fun NavController.navigateToPaymentWalletAddress(
    navOptions: NavOptions? = null,
) {
    navigate(paymentWalletAddressRoute, navOptions)
}