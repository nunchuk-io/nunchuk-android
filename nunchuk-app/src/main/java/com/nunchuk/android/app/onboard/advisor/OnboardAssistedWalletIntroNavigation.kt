package com.nunchuk.android.app.onboard.advisor

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.app.onboard.intro.OnboardIntroScreen
import com.nunchuk.android.app.onboard.intro.onboardIntroRoute

const val onboardAssistedWalletIntroRoute = "onboard_assisted_wallet_intro"
fun NavGraphBuilder.onboardAssistedWalletIntro(
    onSkip: () -> Unit = {},
    openOnboardAdvisorInputScreen: () -> Unit = {},
    onOpenOnboardAdvisorIntroScreen: () -> Unit = {},
) {
    composable(onboardAssistedWalletIntroRoute) {
        OnboardAssistedWalletIntroScreen(
            onSkip = onSkip,
            onOpenOnboardAdvisorInputScreen = openOnboardAdvisorInputScreen,
            onOpenOnboardAdvisorIntroScreen = onOpenOnboardAdvisorIntroScreen
        )
    }
}

fun NavController.navigateToOnboardAssistedWalletIntro() {
    navigate(onboardAssistedWalletIntroRoute)
}