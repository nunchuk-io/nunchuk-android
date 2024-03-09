package com.nunchuk.android.app.onboard.advisor

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.app.onboard.intro.OnboardIntroScreen
import com.nunchuk.android.app.onboard.intro.onboardIntroRoute

const val onboardAdvisorInputRoute = "onboard_advisor_input"
fun NavGraphBuilder.onboardAdvisorInput(
    onSkip: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    composable(onboardAdvisorInputRoute) {
        OnboardAdvisorInputScreen(
            onSkip = onSkip,
            onSignIn = onSignIn
        )
    }
}

fun NavController.navigateToOnboardAdvisorInput() {
    navigate(onboardAdvisorInputRoute)
}