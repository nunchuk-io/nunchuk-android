package com.nunchuk.android.main.groupwallet

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.membership.replacekey.ReplaceKeyIntroScreen

const val replaceWalletIntroRoute = "replaceWalletIntro"


fun NavGraphBuilder.replaceWalletIntroNavigation(
    viewModel: FreeGroupWalletViewModel,
    onContinueClicked: () -> Unit,
    snackState: SnackbarHostState
) {
    composable(replaceWalletIntroRoute) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        ReplaceKeyIntroScreen(
            isLoading = state.isLoading,
            onContinueClicked = onContinueClicked,
            snackState = snackState
        )
    }
}