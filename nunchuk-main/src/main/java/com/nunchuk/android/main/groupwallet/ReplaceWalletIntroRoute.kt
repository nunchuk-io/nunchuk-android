package com.nunchuk.android.main.groupwallet

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.replacekey.ReplaceKeyIntroScreen
import kotlinx.serialization.Serializable

@Serializable
data object ReplaceWalletIntroRoute


fun NavGraphBuilder.replaceWalletIntroNavigation(
    viewModel: FreeGroupWalletViewModel,
    onContinueClicked: () -> Unit,
    snackState: SnackbarHostState
) {
    composable<ReplaceWalletIntroRoute> {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val withTimelock = state.isReplaceKeyWithTimelock
        ReplaceKeyIntroScreen(
            isLoading = state.isLoading,
            titleRes = if (withTimelock) R.string.nc_replace_key_change_timelock else R.string.nc_replace_key_title,
            descRes = if (withTimelock) R.string.nc_replace_key_change_timelock_desc else R.string.nc_replace_key_intro_desc_simple,
            noteRes = if (withTimelock) R.string.nc_replace_key_change_timelock_note else R.string.nc_replace_key_intro_note_simple,
            onContinueClicked = onContinueClicked,
            snackState = snackState
        )
    }
}
