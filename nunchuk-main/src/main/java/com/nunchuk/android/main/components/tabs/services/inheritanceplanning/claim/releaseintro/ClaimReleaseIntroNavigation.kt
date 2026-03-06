package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.releaseintro

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object ClaimReleaseIntroRoute

fun NavGraphBuilder.claimReleaseIntro(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    composable<ClaimReleaseIntroRoute> {
        ClaimReleaseIntroScreen(
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToClaimReleaseIntro() {
    navigate(ClaimReleaseIntroRoute)
}
