package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object NoInheritancePlanFoundRoute

fun NavGraphBuilder.noInheritancePlanFound(
    onCloseClick: () -> Unit = {},
) {
    composable<NoInheritancePlanFoundRoute> {
        NoInheritancePlanFoundScreen(
            onCloseClick = onCloseClick,
        )
    }
}

fun NavController.navigateToNoInheritancePlanFound() {
    navigate(NoInheritancePlanFoundRoute)
}

