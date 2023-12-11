package com.nunchuk.android.main.membership.byzantine.payment.qr

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val qrDetailRoute = "qr_detail/{address}"
fun NavGraphBuilder.addQRDetail() {
    composable(route = qrDetailRoute, arguments = listOf(navArgument("address") {
        type = NavType.StringType
    })) { backStackEntry ->
        val address = backStackEntry.arguments?.getString("address").orEmpty()
        QrDetailRoute(address = address)
    }
}

fun NavController.navigateToQRDetail(
    address: String,
) {
    navigate(route = "qr_detail/${address}")
}