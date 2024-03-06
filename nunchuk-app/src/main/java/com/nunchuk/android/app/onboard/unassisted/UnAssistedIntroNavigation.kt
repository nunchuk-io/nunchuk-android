package com.nunchuk.android.app.onboard.unassisted

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val unassistedIntroRoute = "unassisted_intro"
fun NavGraphBuilder.unassistedIntro() {
    composable(unassistedIntroRoute) {
        UnAssistedIntroScreen()
    }
}

fun NavController.navigateToUnassistedIntro() {
    navigate(unassistedIntroRoute)
}