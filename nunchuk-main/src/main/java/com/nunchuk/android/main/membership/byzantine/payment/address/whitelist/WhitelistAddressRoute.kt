package com.nunchuk.android.main.membership.byzantine.payment.address.whitelist

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel

const val whitelistAddressRoute = "whitelist_address"

fun NavGraphBuilder.addWhitelistAddress(
    paymentViewModel: RecurringPaymentViewModel,
    openPaymentFrequencyScreen: () -> Unit,
) {
    composable(whitelistAddressRoute) {
        WhitelistAddressRoute(
            openPaymentFrequencyScreen = openPaymentFrequencyScreen,
            viewModel = paymentViewModel,
        )
    }
}

fun NavController.navigateToWhitelistAddress(navOptions: NavOptions? = null) {
    navigate(whitelistAddressRoute, navOptions)
}