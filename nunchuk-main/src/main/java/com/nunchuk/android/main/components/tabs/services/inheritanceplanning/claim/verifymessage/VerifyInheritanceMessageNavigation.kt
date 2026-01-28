package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

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
import com.nunchuk.android.nav.NunchukNavigator
import kotlinx.serialization.Serializable

@Serializable
data object VerifyInheritanceMessageRoute

fun NavGraphBuilder.verifyInheritanceMessage(
    snackState: SnackbarHostState,
    navigator: NunchukNavigator,
    onBackPressed: () -> Unit = {},
    addMoreSigner: () -> Unit = {},
    onNavigateToExportComplete: () -> Unit = {},
) {
    composable<VerifyInheritanceMessageRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()
        val uiState by activityViewModel.uiState.collectAsStateWithLifecycle()

        if (claimData.challenge != null) {
            VerifyInheritanceMessageScreen(
                snackState = snackState,
                claimData = claimData,
                navigator = navigator,
                sharedUiState = uiState,
                onBackPressed = onBackPressed,
                addMoreSigner = addMoreSigner,
                onSuccess = { inheritanceAdditional ->
                    activityViewModel.updateInheritanceAdditional(inheritanceAdditional)
                },
                onSigned = activityViewModel::addSignature,
                onNavigateToExportComplete = onNavigateToExportComplete,
                onEventHandled = activityViewModel::onEventHandled
            )
        }
    }
}

fun NavController.navigateToVerifyInheritanceMessage() {
    navigate(VerifyInheritanceMessageRoute)
}
