package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.claimnote

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ClaimNoteRoute

fun NavGraphBuilder.claimNote(
    snackState: SnackbarHostState,
    onDoneClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {},
    onViewWallet: () -> Unit = {},
) {
    composable<ClaimNoteRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        claimData.inheritanceAdditional?.let { inheritanceAdditional ->
            ClaimNoteScreen(
                snackState = snackState,
                inheritanceAdditional = inheritanceAdditional,
                onDoneClick = onDoneClick,
                isOnChainClaim = claimData.isOnChainClaim,
                onWithdrawClick = onWithdrawClick,
                onViewWallet = onViewWallet
            )
        }
    }
}

fun NavController.navigateToClaimNote(navOptions: NavOptions? = null) {
    navigate(ClaimNoteRoute, navOptions)
}

