package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.keycount

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ClaimKeyCountRoute

fun NavGraphBuilder.claimKeyCount(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: (Int) -> Unit = {},
) {
    composable<ClaimKeyCountRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel = hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        ClaimKeyCountScreen(
            keyCounts = claimData.requiredKeyCounts,
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

fun NavController.navigateToClaimKeyCount() {
    navigate(ClaimKeyCountRoute)
}
