package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import com.nunchuk.android.model.InheritanceAdditional
import kotlinx.serialization.Serializable

@Serializable
data object ClaimBackupPasswordRoute

fun NavGraphBuilder.claimBackupPassword(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onNoInheritancePlanFound: () -> Unit = {},
    onSuccess: (
        signers: List<SignerModel>,
        magic: String,
        inheritanceAdditional: InheritanceAdditional,
    ) -> Unit = { _, _, _ -> },
    onSignersFromBackup: (List<SignerModel>) -> Unit = {},
) {
    composable<ClaimBackupPasswordRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        ClaimBackupPasswordScreen(
            snackState = snackState,
            claimData = claimData,
            onBackPressed = onBackPressed,
            onNoInheritancePlanFound = onNoInheritancePlanFound,
            onSuccess = onSuccess,
            onSignersFromBackup = onSignersFromBackup,
        )
    }
}

fun NavController.navigateToClaimBackupPassword() {
    navigate(ClaimBackupPasswordRoute)
}

