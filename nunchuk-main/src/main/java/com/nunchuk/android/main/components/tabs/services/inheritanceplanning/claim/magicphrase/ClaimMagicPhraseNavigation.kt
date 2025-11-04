package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.model.InheritanceClaimingInit
import kotlinx.serialization.Serializable

@Serializable
data object ClaimMagicPhraseRoute

fun NavGraphBuilder.claimMagicPhrase(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: (String, InheritanceClaimingInit) -> Unit,
) {
    composable<ClaimMagicPhraseRoute> {
        ClaimMagicPhraseScreen(
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

