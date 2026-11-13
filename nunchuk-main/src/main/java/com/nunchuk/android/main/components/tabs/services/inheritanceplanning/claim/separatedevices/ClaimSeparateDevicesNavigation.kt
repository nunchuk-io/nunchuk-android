package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.separatedevices

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
internal data class ClaimSeparateDevicesRoute(val keyCount: Int)

fun NavGraphBuilder.claimSeparateDevices(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: (Int) -> Unit = {},
) {
    composable<ClaimSeparateDevicesRoute> { backStackEntry ->
        val keyCount = backStackEntry.toRoute<ClaimSeparateDevicesRoute>().keyCount
        ClaimSeparateDevicesScreen(
            keyCount = keyCount,
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = { onContinue(keyCount) },
        )
    }
}

fun NavController.navigateToClaimSeparateDevices(keyCount: Int) {
    navigate(ClaimSeparateDevicesRoute(keyCount))
}
