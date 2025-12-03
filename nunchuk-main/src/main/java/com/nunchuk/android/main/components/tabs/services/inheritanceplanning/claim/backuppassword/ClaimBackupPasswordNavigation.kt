package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.InheritanceAdditional
import kotlinx.serialization.Serializable

@Serializable
data class ClaimBackupPasswordRoute(
    val magicPhrase: String
)

fun NavGraphBuilder.claimBackupPassword(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onNoInheritancePlanFound: () -> Unit = {},
    onSuccess: (
        signers: List<SignerModel>,
        magic: String,
        inheritanceAdditional: InheritanceAdditional,
        derivationPaths: List<String>
    ) -> Unit = { _, _, _, _ -> },
) {
    composable<ClaimBackupPasswordRoute> {
        val route = it.toRoute<ClaimBackupPasswordRoute>()
        ClaimBackupPasswordScreen(
            snackState = snackState,
            magicPhrase = route.magicPhrase,
            onBackPressed = onBackPressed,
            onNoInheritancePlanFound = onNoInheritancePlanFound,
            onSuccess = onSuccess,
        )
    }
}

fun NavController.navigateToClaimBackupPassword(magicPhrase: String) {
    navigate(ClaimBackupPasswordRoute(magicPhrase = magicPhrase))
}

