package com.nunchuk.android.app.onboard.unassisted

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val unassistedIntroRoute = "unassisted_intro"
fun NavGraphBuilder.unassistedIntro(
    openMainScreen: () -> Unit = {},
) {
    composable(unassistedIntroRoute) {
        UnAssistedIntroScreen(
            openMainScreen = openMainScreen
        )
    }
}

fun NavController.navigateToUnassistedIntro() {
    navigate(unassistedIntroRoute)
}