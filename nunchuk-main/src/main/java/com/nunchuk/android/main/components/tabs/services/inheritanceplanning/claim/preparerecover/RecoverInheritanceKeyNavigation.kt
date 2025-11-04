package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PrepareRecoverInheritanceKeyRoute

fun NavGraphBuilder.recoverInheritanceKey(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: (Boolean) -> Unit = {},
) {
    composable<PrepareRecoverInheritanceKeyRoute> {
        RecoverInheritanceKeyScreen(
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

fun NavController.navigateToRecoverInheritanceKey() {
    navigate(PrepareRecoverInheritanceKeyRoute)
}
