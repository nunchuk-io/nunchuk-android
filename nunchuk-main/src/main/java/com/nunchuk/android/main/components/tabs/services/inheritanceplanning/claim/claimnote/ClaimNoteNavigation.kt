package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.claimnote

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ClaimNoteRoute

fun NavGraphBuilder.claimNote(
    onDoneClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {},
) {
    composable<ClaimNoteRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        claimData.inheritanceAdditional?.let { inheritanceAdditional ->
            ClaimNoteScreen(
                inheritanceAdditional = inheritanceAdditional,
                onDoneClick = {
                    onDoneClick()
                },
                onWithdrawClick = onWithdrawClick,
            )
        }
    }
}

fun NavController.navigateToClaimNote() {
    navigate(ClaimNoteRoute)
}

