package com.nunchuk.android.main.groupwallet

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.replacekey.ReplaceKeyIntroScreen

const val replaceWalletIntroRoute = "replaceWalletIntro"


fun NavGraphBuilder.replaceWalletIntroNavigation(
    onContinueClicked: () -> Unit
) {
    composable(replaceWalletIntroRoute) {
        ReplaceKeyIntroScreen(onContinueClicked = onContinueClicked)
    }
}