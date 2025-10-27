package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AddInheritanceKeyRoute

fun NavGraphBuilder.addInheritanceKey(
    onBackPressed: () -> Unit = {},
    onAddKeyClick: () -> Unit = {},
) {
    composable<AddInheritanceKeyRoute> {
        AddInheritanceKeyScreen(
            onBackPressed = onBackPressed,
            onAddKeyClick = onAddKeyClick,
        )
    }
}

fun NavController.navigateToAddInheritanceKey() {
    navigate(AddInheritanceKeyRoute)
}
