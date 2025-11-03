package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import com.nunchuk.android.model.InheritanceClaimingInit
import kotlinx.serialization.Serializable

@Serializable
data object ClaimMagicPhraseRoute

fun NavGraphBuilder.claimMagicPhrase(
    onBackPressed: () -> Unit = {},
    onContinue: (String, InheritanceClaimingInit) -> Unit,
    sharedViewModel: ClaimInheritanceViewModel,
) {
    composable<ClaimMagicPhraseRoute> {
        val sharedUiState by sharedViewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(sharedUiState.message) {
            if (sharedUiState.message.isNotEmpty()) {
                snackbarHostState.showNunchukSnackbar(
                    message = sharedUiState.message,
                    type = NcToastType.ERROR
                )
                sharedViewModel.handledMessageShown()
            }
        }

        ClaimMagicPhraseScreen(
            snackState = snackbarHostState,
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

