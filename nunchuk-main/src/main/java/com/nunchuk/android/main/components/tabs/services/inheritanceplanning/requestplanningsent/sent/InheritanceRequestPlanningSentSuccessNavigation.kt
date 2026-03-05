package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.sent

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceRequestPlanningSentSuccessRoute

fun NavGraphBuilder.inheritanceRequestPlanningSentSuccess(
    onGotItClick: () -> Unit,
) {
    composable<InheritanceRequestPlanningSentSuccessRoute> {
        InheritanceRequestPlanningSentSuccessScreen(
            onGotItClick = onGotItClick,
        )
    }
}

fun NavController.navigateToInheritanceRequestPlanningSentSuccess() { navigate(InheritanceRequestPlanningSentSuccessRoute) }
