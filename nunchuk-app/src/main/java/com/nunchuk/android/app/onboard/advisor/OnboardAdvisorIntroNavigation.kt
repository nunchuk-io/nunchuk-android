package com.nunchuk.android.app.onboard.advisor

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val onboardAdvisorIntroRoute = "onboard_advisor_intro"
fun NavGraphBuilder.onboardAdvisorIntro(
    onSkip: () -> Unit = {},
    onSignIn: () -> Unit = {},
    navigateToOnboardAdvisorInput: () -> Unit
) {
    composable(onboardAdvisorIntroRoute) {
        OnboardAdvisorIntroScreen(
            onSkip = onSkip,
            onSignIn = onSignIn,
            onOpenOnboardAdvisorInputScreen = { navigateToOnboardAdvisorInput() }
        )
    }
}

fun NavController.navigateToOnboardAdvisorIntro() {
    navigate(onboardAdvisorIntroRoute)
}