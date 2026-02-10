package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceErrorRoute(
    val title: String,
    val message: String
)

fun NavGraphBuilder.inheritanceError(
    snackState: SnackbarHostState,
    onCloseClick: () -> Unit = {},
) {
    composable<InheritanceErrorRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<InheritanceErrorRoute>()
        InheritanceErrorScreen(
            snackState = snackState,
            onCloseClick = onCloseClick,
            title = route.title,
            customMessage = route.message,
        )
    }
}

fun NavController.navigateToInheritanceError(title: String, message: String) {
    navigate(InheritanceErrorRoute(title = title, message = message))
}

