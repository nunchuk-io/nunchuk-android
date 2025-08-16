package com.nunchuk.android.main.groupwallet.recover

import android.app.Activity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
import com.nunchuk.android.core.data.model.GroupWalletDataComposer
import com.nunchuk.android.core.data.model.getWalletConfigTypeBy
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.ADD_WALLET_RESULT
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.FreeGroupWalletActivity
import com.nunchuk.android.main.groupwallet.component.WalletInfo
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.AddWalletArgs
import com.nunchuk.android.type.SignerType

const val freeGroupWalletRecoverRoute = "free_group_wallet_recover/{wallet_id}/{file_path}/{qr_list}"

fun NavGraphBuilder.freeGroupWalletRecover(
    navigator: NunchukNavigator,
    walletId: String,
    filePath: String,
    qrList: List<String>,
    onAddNewKey: (String, List<SupportedSigner>) -> Unit = { _, _ -> },
    finishScreen: () -> Unit,
    onOpenWalletDetail: (String) -> Unit = {},
) {
    composable(
        route = freeGroupWalletRecoverRoute,
        arguments = listOf(
            navArgument(
                name = FreeGroupWalletActivity.EXTRA_WALLET_ID
            ) {
                type = NavType.StringType
                defaultValue = walletId
            },
            navArgument(
                name = FreeGroupWalletActivity.EXTRA_FILE_PATH
            ) {
                type = NavType.StringType
                defaultValue = filePath
            },
            navArgument(
                name = FreeGroupWalletActivity.EXTRA_QR_LIST
            ) {
                type = NavType.StringListType
                defaultValue = qrList
            }
        )
    ) {
        val viewModel = hiltViewModel<FreeGroupWalletRecoverViewModel>()

        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackState = remember { SnackbarHostState() }
        val context = LocalContext.current

        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val data = it.data
                if (it.resultCode == Activity.RESULT_OK && data != null) {
                    val updatedWalletName = data.getStringExtra(ADD_WALLET_RESULT) ?: ""
                    viewModel.updateWalletName(updatedWalletName)
                }
            }

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
                is FreeGroupWalletRecoverEvent.RecoverSuccess -> {
                    val walletName =
                        (state.event as FreeGroupWalletRecoverEvent.RecoverSuccess).walletName
                    NcToastManager.scheduleShowMessage(
                        message = context.getString(R.string.nc_has_been_recovered, walletName),
                    )
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
                onAddNewKey(viewModel.walletId, viewModel.getSuggestedSigners())
            },
            onContinueClicked = {
                viewModel.recoverGroupWallet()
            },
            onGotItClick = {
                viewModel.showAddKeyErrorDialogHandled()
            },
            onEditClicked = {
                viewModel.getWallet()?.let { wallet ->
                    navigator.openAddWalletScreen(
                        activityContext = context,
                        launcher = launcher,
                        args = AddWalletArgs(
                            decoyPin = "",
                            groupWalletComposer = GroupWalletDataComposer(
                                walletName = wallet.name,
                                addressType = wallet.addressType,
                                requireKeys = wallet.totalRequireSigns,
                                totalKeys = wallet.signers.size,
                                walletConfigType = getWalletConfigTypeBy(
                                    wallet.signers.size,
                                    wallet.totalRequireSigns
                                )
                            )
                        ),
                    )
                }
            },
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
                    IconButton(onClick = { showAskForDeleteDialog = true }) {
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
                            text = stringResource(id = R.string.nc_recover_group_wallet),
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
                    Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                }
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { onContinueClicked() },
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
                    walletType = null,
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
                title = stringResource(id = R.string.nc_incorrect_key),
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
                        item = signer.copy(
                            name = stringResource(
                                R.string.nc_key_with_index,
                                "#${index + 1}"
                            ),
                        ),
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

