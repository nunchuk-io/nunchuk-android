package com.nunchuk.android.app.onboard.intro

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val onboardIntroRoute = "onboard_intro"
fun NavGraphBuilder.onboardIntro(
    onOpenUnassistedIntro: () -> Unit,
    onOpenAssistedIntro: () -> Unit,
    openMainScreen: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    composable(onboardIntroRoute) {
        OnboardIntroScreen(
            onOpenUnassistedIntro = onOpenUnassistedIntro,
            onOpenAssistedIntro = onOpenAssistedIntro,
            openMainScreen = openMainScreen,
            onSignIn = onSignIn,
        )
    }
}

fun NavController.navigateToOnboardIntro() {
    navigate(onboardIntroRoute)
}