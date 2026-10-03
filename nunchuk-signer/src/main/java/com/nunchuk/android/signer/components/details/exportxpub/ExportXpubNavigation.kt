package com.nunchuk.android.signer.components.details.exportxpub

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.HardwareTaprootSupportViewModel
import com.nunchuk.android.signer.trezor.SelectWalletTypeScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
internal data object ExportXpubSelectAccountRoute

@Serializable
internal data object ExportXpubCustomPathRoute

@Serializable
internal data object ExportXpubRoute

internal fun NavGraphBuilder.exportXpubSelectAccount(
    viewModel: ExportXpubViewModel,
    taprootSupportViewModel: HardwareTaprootSupportViewModel,
    onBack: () -> Unit,
) {
    composable<ExportXpubSelectAccountRoute> {
        val taprootSupportState by taprootSupportViewModel.state.collectAsStateWithLifecycle()
        val args = viewModel.args
        SelectWalletTypeScreen(
            title = stringResource(R.string.nc_export_xpub),
            subtitle = stringResource(R.string.nc_export_xpub_select_account),
            isTaprootSupported = { walletType ->
                taprootSupportState.isTaprootSupported(args.signerType, args.signerTags, walletType)
            },
            onBack = onBack,
            onCustomPathClicked = viewModel::openCustomPath,
            onContinue = viewModel::exportPreset,
        )
    }
}

internal fun NavGraphBuilder.exportXpubCustomPath(
    viewModel: ExportXpubViewModel,
    onBack: () -> Unit,
) {
    composable<ExportXpubCustomPathRoute> {
        val state by viewModel.state.collectAsStateWithLifecycle()
        ExportXpubCustomPathContent(
            path = state.customPath,
            isPathValid = state.isCustomPathValid,
            onPathChanged = viewModel::onCustomPathChanged,
            onBack = onBack,
            onContinue = viewModel::exportCustomPath,
        )
    }
}

internal fun NavGraphBuilder.exportXpub(
    viewModel: ExportXpubViewModel,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
) {
    composable<ExportXpubRoute> {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val copiedMessage = stringResource(R.string.nc_xpub_copied)
        ExportXpubContent(
            descriptor = state.descriptor,
            snackState = snackState,
            onBack = onBack,
            onCopy = {
                onCopy(state.descriptor)
                // Above the bottom bar, so the toast never covers Copy XPUB.
                scope.launch { snackState.showNunchukSnackbar(copiedMessage) }
            },
        )
    }
}

internal fun NavController.navigateToExportXpubCustomPath() {
    navigate(ExportXpubCustomPathRoute)
}

internal fun NavController.navigateToExportXpub() {
    navigate(ExportXpubRoute) { launchSingleTop = true }
}
