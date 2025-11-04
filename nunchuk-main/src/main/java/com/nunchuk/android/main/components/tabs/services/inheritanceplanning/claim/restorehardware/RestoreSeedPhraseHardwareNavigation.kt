package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object RestoreSeedPhraseHardwareRoute

fun NavGraphBuilder.restoreSeedPhraseHardware(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    composable<RestoreSeedPhraseHardwareRoute> {
        RestoreSeedPhraseHardwareScreen(
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

fun NavController.navigateToRestoreSeedPhraseHardware() {
    navigate(RestoreSeedPhraseHardwareRoute)
}


