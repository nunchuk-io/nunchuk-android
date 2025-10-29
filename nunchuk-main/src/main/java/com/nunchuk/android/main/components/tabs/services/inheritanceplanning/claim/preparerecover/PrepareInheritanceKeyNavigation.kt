package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PrepareInheritanceKeyRoute

fun NavGraphBuilder.prepareInheritanceKey(
    onBackPressed: () -> Unit = {},
    onContinue: (InheritanceOption) -> Unit = {},
) {
    composable<PrepareInheritanceKeyRoute> {
        PrepareInheritanceKeyScreen(
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

fun NavController.navigateToPrepareInheritanceKey() {
    navigate(PrepareInheritanceKeyRoute)
}

