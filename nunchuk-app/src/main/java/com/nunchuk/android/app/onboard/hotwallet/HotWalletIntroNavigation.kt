package com.nunchuk.android.app.onboard.hotwallet

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val hotWalletIntroRoute = "hotWalletIntroRoute"

fun NavGraphBuilder.hotWalletIntro(
    returnToMainScreen: () -> Unit = {},
    openServiceTab: () -> Unit = {},
) {
    composable(hotWalletIntroRoute) {
        HotWalletIntroScreen(
            returnToMainScreen = returnToMainScreen,
            openServiceTab = openServiceTab
        )
    }
}

fun NavController.navigateToHotWalletIntro(
    navOptions: NavOptions? = null,
) {
    navigate(hotWalletIntroRoute, navOptions)
}