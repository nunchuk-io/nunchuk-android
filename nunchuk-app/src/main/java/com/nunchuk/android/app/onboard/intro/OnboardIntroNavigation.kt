package com.nunchuk.android.app.onboard.intro

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val onboardIntroRoute = "onboard_intro"
fun NavGraphBuilder.onboardIntro() {
    composable(onboardIntroRoute) {
        OnboardIntroScreen()
    }
}

fun NavController.navigateToOnboardIntro() {
    navigate(onboardIntroRoute)
}