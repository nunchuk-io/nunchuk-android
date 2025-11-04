package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PrepareInheritanceKeyRoute

fun NavGraphBuilder.prepareInheritanceKey(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: (InheritanceOption) -> Unit = {},
) {
    composable<PrepareInheritanceKeyRoute> {
        PrepareInheritanceKeyScreen(
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

fun NavController.navigateToPrepareInheritanceKey() {
    navigate(PrepareInheritanceKeyRoute)
}

