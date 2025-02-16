package com.nunchuk.android.main.groupwallet

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
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
import com.nunchuk.android.compose.NcIcon
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
import com.nunchuk.android.compose.pullrefresh.PullRefreshIndicator
import com.nunchuk.android.compose.pullrefresh.pullRefresh
import com.nunchuk.android.compose.pullrefresh.rememberPullRefreshState
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.component.FreeAddKeyCard
import com.nunchuk.android.main.groupwallet.component.UserOnline
import com.nunchuk.android.main.groupwallet.component.WalletInfo
import com.nunchuk.android.main.membership.key.list.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.type.SignerType

const val freeGroupWalletRoute = "free_group_wallet"
val avatarColors = listOf(
    Color(0xFF1C652D),
    Color(0xFFA66800),
    Color(0xFFCF4018),
    Color(0xFF7E519B),
    Color(0xFF2F466C),
    Color(0xFFF1AE00),
    Color(0xFF757575),
)

fun NavGraphBuilder.freeGroupWallet(
    viewModel: FreeGroupWalletViewModel,
    onEditClicked: (String, Boolean) -> Unit = { _, _ -> },
    onCopyLinkClicked: (String) -> Unit = {},
    onShowQRCodeClicked: (String) -> Unit = {},
    onAddNewKey: (Int) -> Unit = {},
    onAddExistingKey: (SignerModel, Int) -> Unit,
    finishScreen: () -> Unit,
    returnToHome: () -> Unit,
    onContinueClicked: (GroupSandbox) -> Unit = {},
    onStartAddKey: (Int) -> Unit = {},
    onChangeBip32Path: (Int, SignerModel) -> Unit = { _, _ -> },
    refresh: () -> Unit,
) {
    composable(
        route = freeGroupWalletRoute,
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
            viewModel.getGroupSandbox()
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

        FreeGroupWalletScreen(
            snackState = snackState,
            state = state,
            onAddNewKey = onAddNewKey,
            onContinueClicked = onContinueClicked,
            onEditClicked = {
                state.group?.let {
                    onEditClicked(it.id, state.signers.any { it != null })
                }
            },
            onCopyLinkClicked = onCopyLinkClicked,
            onShowQRCodeClicked = onShowQRCodeClicked,
            onRemoveClicked = viewModel::removeSignerFromGroup,
            onAddExistingKey = onAddExistingKey,
            onDeleteGroupClicked = viewModel::deleteGroupSandbox,
            returnToHome = returnToHome,
            onStartAddKey = onStartAddKey,
            onChangeBip32Path = onChangeBip32Path,
            refresh = refresh
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGroupWalletScreen(
    state: FreeGroupWalletUiState = FreeGroupWalletUiState(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onAddNewKey: (Int) -> Unit = {},
    onRemoveClicked: (Int) -> Unit = {},
    onContinueClicked: (GroupSandbox) -> Unit = {},
    onEditClicked: () -> Unit = {},
    onCopyLinkClicked: (String) -> Unit = {},
    onShowQRCodeClicked: (String) -> Unit = {},
    onAddExistingKey: (SignerModel, Int) -> Unit = { _, _ -> },
    onDeleteGroupClicked: () -> Unit = {},
    returnToHome: () -> Unit = {},
    onStartAddKey: (Int) -> Unit = {},
    onChangeBip32Path: (Int, SignerModel) -> Unit = { _, _ -> },
    refresh: () -> Unit = {},
) {
    val pullRefreshState = rememberPullRefreshState(state.isRefreshing, refresh)
    var showSignerBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showMoreOption by rememberSaveable { mutableStateOf(false) }
    var showAskForDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var currentSignerIndex by rememberSaveable { mutableIntStateOf(-1) }
    var showDeleteSignerDialog by rememberSaveable { mutableStateOf(false) }
    var showKeyNotSynced by rememberSaveable { mutableStateOf(false) }
    var showBip32Path by rememberSaveable { mutableStateOf(false) }
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
                onClick = {
                    if (state.signers.any { it?.name == KEY_NOT_SYNCED_NAME }) {
                        showKeyNotSynced = true
                    } else {
                        onContinueClicked(state.group!!)
                    }
                },
                enabled = state.group != null && state.signers.count { it != null } == state.group.n && state.group.n > 0,
            ) {
                Text(text = stringResource(id = R.string.nc_wallet_create_wallet))
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WalletInfo(
                        name = state.group?.name ?: "",
                        requireSigns = state.group?.m ?: 0,
                        totalSigns = state.group?.n ?: 0,
                        addressType = state.group?.addressType,
                        onEditClicked = onEditClicked,
                        onCopyLinkClicked = {
                            state.group?.let { onCopyLinkClicked(it.url) }
                        },
                        onShowQRCodeClicked = {
                            state.group?.let { onShowQRCodeClicked(it.url) }
                        }
                    )
                }

                item {
                    UserOnline(state.numberOfOnlineUsers)
                }

                itemsIndexed(state.signers) { index, signer ->
                    FreeAddKeyCard(
                        index = index,
                        isOccupied = state.occupiedSlotsIndex.contains(index),
                        signer = signer,
                        onAddClicked = {
                            currentSignerIndex = index
                            onStartAddKey(index)
                            if (state.allSigners.isNotEmpty()) {
                                showSignerBottomSheet = true
                            } else {
                                onAddNewKey(index)
                            }
                        },
                        onRemoveClicked = {
                            currentSignerIndex = index
                            showDeleteSignerDialog = true
                        },
                        showBip32Path = showBip32Path,
                        onChangeBip32Path = onChangeBip32Path
                    )
                }
            }

            PullRefreshIndicator(state.isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }

        if (showMoreOption) {
            NcSelectableBottomSheet(
                options = listOf(
                    if (showBip32Path) stringResource(R.string.nc_hide_bip_32_path) else stringResource(R.string.nc_show_bip_32_path),
                    stringResource(R.string.nc_cancel_group_wallet_setup),
                ),
                onSelected = {
                    if (it == 0) {
                        showBip32Path = !showBip32Path
                    } else {
                        showAskForDeleteDialog = true
                    }
                },
                onDismiss = {
                    showMoreOption = false
                },
            )
        }

        if (showSignerBottomSheet) {
            val addedSigners = state.signers.filterNotNull().map { it.fingerPrint }.toSet()
            val allSigners = state.allSigners.filter {
                !addedSigners.contains(it.fingerPrint)
            }
            if (allSigners.isNotEmpty()) {
                SelectSignerBottomSheet(
                    onDismiss = { showSignerBottomSheet = false },
                    supportedSigners = state.supportedTypes.takeIf { state.group?.addressType?.isTaproot() == true }
                        .orEmpty(),
                    onAddExistKey = {
                        showSignerBottomSheet = false
                        onAddExistingKey(it, currentSignerIndex)
                    },
                    onAddNewKey = {
                        showSignerBottomSheet = false
                        onAddNewKey(currentSignerIndex)
                    },
                    args = TapSignerListBottomSheetFragmentArgs(
                        signers = allSigners.toTypedArray(),
                        type = SignerType.UNKNOWN
                    )
                )
            } else {
                showSignerBottomSheet = false
                onAddNewKey(currentSignerIndex)
            }
        }

        if (showAskForDeleteDialog) {
            NcConfirmationDialog(
                message = stringResource(id = R.string.nc_ask_for_delete_group_wallet),
                onPositiveClick = {
                    onDeleteGroupClicked()
                    showAskForDeleteDialog = false
                },
                onDismiss = {
                    showAskForDeleteDialog = false
                }
            )
        }

        if (state.groupWalletUnavailable) {
            NcInfoDialog(
                title = stringResource(id = R.string.nc_unable_access_link),
                message = stringResource(id = R.string.nc_group_wallet_created_by_others),
                onPositiveClick = {
                    returnToHome()
                },
                onDismiss = {
                    returnToHome()
                },
                positiveButtonText = stringResource(R.string.nc_return_to_home_screen)
            )
        }
        if (showDeleteSignerDialog) {
            NcConfirmationDialog(
                title = stringResource(id = R.string.nc_text_warning),
                message = stringResource(id = R.string.nc_ask_for_delete_signer),
                onPositiveClick = {
                    onRemoveClicked(currentSignerIndex)
                    showDeleteSignerDialog = false
                },
                onDismiss = {
                    showDeleteSignerDialog = false
                }
            )
        }
        if (showKeyNotSynced) {
            NcInfoDialog(
                title = stringResource(R.string.nc_waiting_for_other_devices),
                message = stringResource(id = R.string.nc_key_not_synced_desc),
                positiveButtonText = stringResource(R.string.nc_ok),
                onPositiveClick = {
                    showKeyNotSynced = false
                },
                onDismiss = {
                    showKeyNotSynced = false
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun GroupWalletScreenPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    val addedSigner = signers.first().copy(name = KEY_NOT_SYNCED_NAME)
    NunchukTheme {
        FreeGroupWalletScreen(
            state = FreeGroupWalletUiState(signers = signers + addedSigner + null)
        )
    }
}

