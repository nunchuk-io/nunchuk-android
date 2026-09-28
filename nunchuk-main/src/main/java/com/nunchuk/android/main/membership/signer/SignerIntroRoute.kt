package com.nunchuk.android.main.membership.signer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SelectSignerArgs
import com.nunchuk.android.core.signer.SelectSignerBottomSheet
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.signer.SignerIntroHostEvent
import com.nunchuk.android.signer.SignerIntroEvent
import com.nunchuk.android.signer.SignerIntroViewModel
import com.nunchuk.android.signer.toSignerTypeAndTag

/** Owns Compose navigation and sheets; device navigation stays at the Activity boundary. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SignerIntroRoute(
    request: SignerIntroRequest,
    viewModel: SignerIntroViewModel,
    onEvent: (SignerIntroHostEvent) -> Unit,
    onSigner: (SignerModel) -> Unit,
    onMore: () -> Unit,
    onRecoverSeed: () -> Unit,
    onRecoverXprv: () -> Unit,
    onRecoverTapsigner: () -> Unit,
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // One consumer for all effects. Buffered events allow verification to start before composition.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.event.collect { event ->
                when (event) {
                    is SignerIntroEvent.CheckFirmware -> navController.navigate(CheckFirmwareDestination(
                        deviceName = event.device.name,
                        walletId = request.walletId,
                        groupId = request.groupId,
                    ))
                    is SignerIntroHostEvent -> onEvent(event)
                }
            }
        }
    }

    NunchukTheme {
        if (viewModel.verifyingKeyType == null) {
            NavHost(
                navController = navController,
                startDestination = if (viewModel.isInheritanceSetup) {
                    InheritanceKeyIntroDestination
                } else SignerIntroDestination,
            ) {
                signerIntroDestination(
                    viewModel = viewModel,
                    onChainAddSignerParam = request.deviceParams,
                    onClick = viewModel::onKeySelected,
                    onMoreClicked = onMore,
                )
                inheritanceKeyIntroDestination(
                    remainTime = remainTime,
                    onMoreClicked = onMore,
                    onContinueClicked = { navController.navigate(InheritancePassphraseNoticeDestination) },
                )
                inheritancePassphraseNoticeDestination(
                    remainTime = remainTime,
                    onMoreClicked = onMore,
                    onContinueClicked = { navController.navigate(SignerIntroDestination) },
                )
                checkFirmwareDestination(
                    onChainAddSignerParam = request.deviceParams,
                    onMoreClicked = onMore,
                    onFilteredSignersReady = onSigner,
                    onOpenNextScreen = viewModel::onFirmwareChecked,
                )
            }
        }

        state.existingSelection?.let { selection ->
            SelectSignerBottomSheet(
                args = SelectSignerArgs(
                    signers = selection.signers,
                    type = selection.keyType.toSignerTypeAndTag().first,
                    description = "",
                    ignoreIndexCheckForAcctX = true,
                ),
                onDismiss = viewModel::dismissExistingSigners,
                onAddExistKey = { signer ->
                    viewModel.dismissExistingSigners()
                    onSigner(signer)
                },
                onAddNewKey = viewModel::onCreateNewSigner,
            )
        }
        if (state.showRecoveryOptions) {
            NcSelectableBottomSheet(
                options = listOf(
                    stringResource(R.string.nc_recover_key_via_seed),
                    stringResource(R.string.nc_recover_key_via_xprv),
                    stringResource(R.string.nc_recover_tapsigner_key_from_backup),
                ),
                onSelected = { index ->
                    viewModel.dismissRecoveryOptions()
                    when (index) {
                        0 -> onRecoverSeed()
                        1 -> onRecoverXprv()
                        2 -> onRecoverTapsigner()
                    }
                },
                onDismiss = viewModel::dismissRecoveryOptions,
            )
        }
    }
}
