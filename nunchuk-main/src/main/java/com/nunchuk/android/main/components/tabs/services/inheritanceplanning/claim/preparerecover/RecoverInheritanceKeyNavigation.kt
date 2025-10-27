package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PrepareRecoverInheritanceKeyRoute

fun NavGraphBuilder.recoverInheritanceKey(
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    composable<PrepareRecoverInheritanceKeyRoute> {
        RecoverInheritanceKeyScreen(
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

fun NavController.navigateToRecoverInheritanceKey() {
    navigate(PrepareRecoverInheritanceKeyRoute)
}
