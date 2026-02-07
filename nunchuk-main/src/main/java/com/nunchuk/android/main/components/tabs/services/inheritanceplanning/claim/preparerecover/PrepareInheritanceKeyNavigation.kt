package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover

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
data object PrepareInheritanceKeyRoute

fun NavGraphBuilder.prepareInheritanceKey(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: (InheritanceOption) -> Unit = {},
) {
    composable<PrepareInheritanceKeyRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        PrepareInheritanceKeyScreen(
            isOnChainClaim = claimData.isOnChainClaim,
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

fun NavController.navigateToPrepareInheritanceKey() {
    navigate(PrepareInheritanceKeyRoute)
}

