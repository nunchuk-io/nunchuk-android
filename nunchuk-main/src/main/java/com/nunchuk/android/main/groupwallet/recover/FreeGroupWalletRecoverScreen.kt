package com.nunchuk.android.main.groupwallet.recover

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.provider.WalletExtendedProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.component.WalletInfo
import com.nunchuk.android.type.SignerType

const val freeGroupWalletRecoverRoute = "free_group_wallet_recover"

fun NavGraphBuilder.freeGroupWalletRecover(
    viewModel: FreeGroupWalletRecoverViewModel,
    onAddNewKey: (Int) -> Unit = {},
    finishScreen: () -> Unit,
    onOpenWalletDetail: (String) -> Unit = {},
    onEditClicked: () -> Unit = {},
) {
    composable(
        route = freeGroupWalletRecoverRoute,
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackState = remember { SnackbarHostState() }

        LaunchedEffect(state.isFinishScreen) {
            if (state.isFinishScreen) {
                finishScreen()
            }
        }

        if (state.isLoading) {
            NcLoadingDialog()
        }

        LifecycleResumeEffect(Unit) {
            viewModel.loadInfo()
            onPauseOrDispose { }
        }

        LaunchedEffect(state.errorMessage) {
            if (state.errorMessage.isNotEmpty()) {
                snackState.showSnackbar(
                    NcSnackbarVisuals(
                        message = state.errorMessage,
                        type = NcToastType.ERROR
                    )
                )
                viewModel.markMessageHandled()
            }
        }

        LaunchedEffect(state.event) {
            when (state.event) {
                FreeGroupWalletRecoverEvent.RecoverSuccess -> {
                    onOpenWalletDetail(state.wallet?.id.orEmpty())
                    finishScreen()
                }
                else -> {
                }
            }
            viewModel.markEventHandled()
        }

        FreeGroupWalletRecoverScreen(
            snackState = snackState,
            state = state,
            onAddNewKey = {
                viewModel.setCurrentSignerIndex(it)
                onAddNewKey(it)
            },
            onContinueClicked = {
                viewModel.recoverGroupWallet()
            },
            onGotItClick = {
                viewModel.showAddKeyErrorDialogHandled()
            },
            onEditClicked = onEditClicked,
            onCancelClicked = {
                finishScreen()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGroupWalletRecoverScreen(
    state: FreeGroupWalletRecoverUiState = FreeGroupWalletRecoverUiState(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onAddNewKey: (Int) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onGotItClick: () -> Unit = {},
    onEditClicked: () -> Unit = {},
    onCancelClicked: () -> Unit = {},
) {
    var showMoreOption by rememberSaveable { mutableStateOf(false) }
    var currentSignerIndex by rememberSaveable { mutableIntStateOf(-1) }
    var showAskForDeleteDialog by rememberSaveable { mutableStateOf(false) }

    NcScaffold(
        snackState = snackState,
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
            CenterAlignedTopAppBar(
                modifier = Modifier,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.textPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.nc_setup_group_wallet),
                            style = NunchukTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            NcIcon(
                                painter = painterResource(id = R.drawable.ic_encrypted),
                                contentDescription = "Encrypted icon",
                                tint = colorResource(id = R.color.nc_text_secondary)
                            )
                            Text(
                                text = stringResource(id = R.string.nc_encrypted),
                                style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                },
                actions = {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.textPrimary) {
                        IconButton(onClick = {
                            showMoreOption = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { onContinueClicked() },
                enabled = state.wallet != null && state.signerUis.count { it.isInDevice } == state.wallet.signers.size,
            ) {
                Text(text = stringResource(id = R.string.nc_recover_wallet))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val requireSigns = state.wallet?.totalRequireSigns ?: 0
                val totalSigns = state.wallet?.signers?.size ?: 0
                WalletInfo(
                    requireSigns = requireSigns,
                    totalSigns = totalSigns,
                    name = state.wallet?.name.orEmpty(),
                    addressType = state.wallet?.addressType,
                    copyLinkEnabled = false,
                    showQRCodeEnabled = false,
                    onEditClicked = onEditClicked
                )
            }

            itemsIndexed(state.signerUis) { index, ui ->
                FreeAddKeyRecoverCard(
                    index = index,
                    signer = ui.signer,
                    isInDevice = ui.isInDevice,
                    onAddClicked = {
                        currentSignerIndex = ui.index
                        onAddNewKey(ui.index)
                    },
                )
            }
        }

        if (state.showAddKeyErrorDialog) {
            NcInfoDialog(
                title = stringResource(id = R.string.nc_error),
                message = stringResource(id = R.string.nc_failed_add_key_correct_key_from_config),
                onPositiveClick = {
                    onGotItClick()
                },
                onDismiss = {
                    onGotItClick()
                },
                positiveButtonText = stringResource(R.string.nc_text_got_it)
            )
        }

        if (showAskForDeleteDialog) {
            NcConfirmationDialog(
                message = stringResource(id = R.string.nc_ask_for_cancel_group_wallet_recovery),
                onPositiveClick = {
                    onCancelClicked()
                    showAskForDeleteDialog = false
                },
                onDismiss = {
                    showAskForDeleteDialog = false
                }
            )
        }

        if (showMoreOption) {
            NcSelectableBottomSheet(
                options = listOf(stringResource(R.string.nc_cancel_group_wallet_recovery)),
                onSelected = {
                    if (it == 0) {
                        showAskForDeleteDialog = true
                    }
                },
                onDismiss = {
                    showMoreOption = false
                },
            )
        }
    }
}

@Composable
fun FreeAddKeyRecoverCard(
    index: Int,
    modifier: Modifier = Modifier,
    signer: SignerModel,
    isInDevice: Boolean = false,
    onAddClicked: () -> Unit,
) {
    if (isInDevice) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.strokePrimary,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SignerCard(item = signer, modifier = Modifier.weight(1.0f))
        }
    } else {
        NcDashLineBox(
            modifier = modifier,
            content = {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignerCard(
                        item = signer.copy(name = stringResource(R.string.nc_key_with_index, "#${index + 1}"),),
                        modifier = Modifier.weight(1.0f),
                        signerIcon = R.drawable.ic_signer_empty_state,
                        isShowKeyTypeBadge = signer.type != SignerType.UNKNOWN,
                    )
                    NcOutlineButton(
                        modifier = Modifier.height(36.dp),
                        onClick = onAddClicked,
                    ) {
                        Text(text = stringResource(id = R.string.nc_add_key))
                    }
                }
            }
        )
    }
}

@PreviewLightDark
@Composable
private fun GroupWalletScreenPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    val walletExtended = WalletExtendedProvider().values.first()
    val signerUis = signers.map { SignerModelRecoverUi(it, 0, false) }
    NunchukTheme {
        FreeGroupWalletRecoverScreen(
            state = FreeGroupWalletRecoverUiState(
                signerUis = signerUis,
                wallet = walletExtended.wallet
            ),
        )
    }
}

