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
import com.nunchuk.android.compose.NcBadgePrimary
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.miniscript.MiniscriptTaproot
import com.nunchuk.android.compose.miniscript.PolicyHeader
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.pullrefresh.PullRefreshIndicator
import com.nunchuk.android.compose.pullrefresh.pullRefresh
import com.nunchuk.android.compose.pullrefresh.rememberPullRefreshState
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.component.FreeAddKeyCard
import com.nunchuk.android.main.groupwallet.component.UserOnline
import com.nunchuk.android.main.groupwallet.component.WalletInfo
import com.nunchuk.android.main.membership.key.list.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import timber.log.Timber

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
    snackState: SnackbarHostState,
    onEditClicked: (String, Boolean, String) -> Unit = { _, _, _ -> },
    onCopyLinkClicked: (String) -> Unit = {},
    onShowQRCodeClicked: (String) -> Unit = {},
    onAddNewKey: (Int) -> Unit = {},
    onAddExistingKey: (SignerModel, Int) -> Unit,
    finishScreen: () -> Unit,
    returnToHome: () -> Unit,
    onContinueClicked: (GroupSandbox) -> Unit = {},
    onStartAddKey: (Int) -> Unit = {},
    onChangeBip32Path: (Int, SignerModel) -> Unit = { _, _ -> },
    openWalletDetail: (String) -> Unit,
    refresh: () -> Unit,
    onAddNewKeyForMiniscript: (List<SupportedSigner>) -> Unit = {},
    onStartAddKeyForMiniscript: (String) -> Unit = {},
) {
    composable(
        route = freeGroupWalletRoute,
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state.isFinishScreen) {
            if (state.isFinishScreen) {
                finishScreen()
            }
        }

        LaunchedEffect(state.finalizedWalletId) {
            if (!state.finalizedWalletId.isNullOrEmpty()) {
                openWalletDetail(state.finalizedWalletId.orEmpty())
            }
        }

        if (state.isLoading) {
            NcLoadingDialog()
        }

        LifecycleResumeEffect(Unit) {
            viewModel.getGroupSandbox()
            onPauseOrDispose { }
        }

        FreeGroupWalletScreen(
            snackState = snackState,
            state = state,
            onAddNewKey = onAddNewKey,
            onContinueClicked = onContinueClicked,
            onEditClicked = {
                state.group?.let {
                    onEditClicked(it.id, state.signers.any { it != null }, it.miniscriptTemplate)
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
            refresh = refresh,
            onAddNewKeyForMiniscript = onAddNewKeyForMiniscript,
            onAddExistingKeyForMiniscript = { signer, keyName ->
                viewModel.addExistingSignerForKey(signer, keyName)
            },
            onSetCurrentKey = { keyName ->
                viewModel.setCurrentKeyToAssign(keyName)
            },
            onStartAddKeyForMiniscript = onStartAddKeyForMiniscript,
            onRemoveSignerForKey = { keyName ->
                viewModel.removeSignerForKey(keyName)
            },
            onMarkEventHandled = {
                viewModel.markEventHandled()
            }
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
    onAddNewKeyForMiniscript: (List<SupportedSigner>) -> Unit = {},
    onAddExistingKeyForMiniscript: (SignerModel, String) -> Unit = { _, _ -> },
    onSetCurrentKey: (String) -> Unit = {},
    onStartAddKeyForMiniscript: (String) -> Unit = {},
    onRemoveSignerForKey: (String) -> Unit = {},
    onMarkEventHandled: () -> Unit = {},
) {
    val pullRefreshState = rememberPullRefreshState(state.isRefreshing, refresh)
    var showSignerBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showMoreOption by rememberSaveable { mutableStateOf(false) }
    var showAskForDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var currentSignerIndex by rememberSaveable { mutableIntStateOf(-1) }
    var showDeleteSignerDialog by rememberSaveable { mutableStateOf(false) }
    var showKeyNotSynced by rememberSaveable { mutableStateOf(false) }
    var showBip32Path by rememberSaveable { mutableStateOf(false) }
    
    // New state variables for key actions
    var showRemoveConfirmation by rememberSaveable { mutableStateOf(false) }
    var keyToRemove by rememberSaveable { mutableStateOf("") }
    var showDuplicateSignerWarning by rememberSaveable { mutableStateOf(false) }
    var duplicateSignerData by rememberSaveable { mutableStateOf<Pair<SignerModel, String>?>(null) }
    
    val isInReplace = state.isInReplaceMode
    val isButtonEnabled = if (state.group != null) {
        val signers = if (isInReplace) state.replaceSigners else state.signers
        signers.count { it != null } == state.group.n && state.group.n > 0
    } else {
        false
    }
    
    // Handle events from ViewModel
    LaunchedEffect(state.event) {
        when (val event = state.event) {
            is FreeGroupWalletEvent.Error -> {
                onMarkEventHandled()
            }
            is FreeGroupWalletEvent.SignerAdded -> {
                onMarkEventHandled()
            }
            is FreeGroupWalletEvent.SignerRemoved -> {
                onMarkEventHandled()
            }
            is FreeGroupWalletEvent.ShowDuplicateSignerWarning -> {
                duplicateSignerData = event.signer to event.keyName
                showDuplicateSignerWarning = true
                onMarkEventHandled()
            }
            null -> {}
        }
    }
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
                    val signers = if (isInReplace) state.replaceSigners else state.signers
                    if (signers.any { it?.name == KEY_NOT_SYNCED_NAME }) {
                        showKeyNotSynced = true
                    } else {
                        onContinueClicked(state.group!!)
                    }
                },
                enabled = isButtonEnabled,
            ) {
                if (state.group != null && state.group.replaceWalletId.isNotEmpty()) {
                    Text(text = stringResource(id = R.string.nc_continue_to_create_a_new_wallet))
                } else {
                    Text(text = stringResource(id = R.string.nc_wallet_create_wallet))
                }
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
                        walletType = state.group?.walletType,
                        onEditClicked = onEditClicked,
                        onCopyLinkClicked = {
                            state.group?.let { onCopyLinkClicked(it.url) }
                        },
                        onShowQRCodeClicked = {
                            state.group?.let { onShowQRCodeClicked(it.url) }
                        }
                    )
                }

                if (state.group?.walletType == WalletType.MINISCRIPT) {

                    item {
                        PolicyHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            showUserAvatars = true,
                            numberOfOnlineUsers = state.numberOfOnlineUsers,
                        )

                        TaprootAddressContent(
                            state = state,
                            showBip32Path = showBip32Path,
                            onChangeBip32Path = onChangeBip32Path,
                            onActionKey = { keyName, signer ->
                                onSetCurrentKey(keyName)
                                if (signer != null) {
                                    keyToRemove = keyName
                                    showRemoveConfirmation = true
                                } else {
                                    onStartAddKeyForMiniscript(keyName)
                                    if (state.allSigners.isNotEmpty()) {
                                        showSignerBottomSheet = true
                                    } else {
                                        onAddNewKeyForMiniscript(state.supportedTypes)
                                    }
                                }
                            }
                        )

                        state.scriptNode?.let { scriptNode ->
                            val startingColorIndex = 0
                            ScriptNodeTree(
                                node = scriptNode,
                                data = ScriptNodeData(
                                    mode = ScriptMode.CONFIG,
                                    signers = state.namedSigners,
                                    showBip32Path = true,
                                    isGroupWallet = true,
                                    occupiedSlots = state.namedOccupied,
                                    colorIndex = startingColorIndex
                                ),
                                onChangeBip32Path = { _, _ -> },
                                onActionKey = { keyPath, signer ->
                                    onSetCurrentKey(keyPath)
                                    if (signer != null) {
                                        keyToRemove = keyPath
                                        showRemoveConfirmation = true
                                    } else {
                                        onStartAddKeyForMiniscript(keyPath)
                                        if (state.allSigners.isNotEmpty()) {
                                            showSignerBottomSheet = true
                                        } else {
                                            onAddNewKeyForMiniscript(state.supportedTypes)
                                        }
                                    }
                                },
                                customActionButton = { key, signer ->
                                    when {
                                        signer == null -> {
                                            NcOutlineButton(
                                                height = 36.dp,
                                                onClick = { 
                                                    onSetCurrentKey(key)
                                                    onStartAddKeyForMiniscript(key)
                                                    if (state.allSigners.isNotEmpty()) {
                                                        showSignerBottomSheet = true
                                                    } else {
                                                        onAddNewKeyForMiniscript(state.supportedTypes)
                                                    }
                                                },
                                            ) {
                                                Text(stringResource(R.string.nc_add))
                                            }
                                        }
                                        
                                        signer.isVisible -> {
                                            NcOutlineButton(
                                                height = 36.dp,
                                                onClick = { 
                                                    onSetCurrentKey(key)
                                                    keyToRemove = key
                                                    showRemoveConfirmation = true
                                                },
                                            ) {
                                                Text(stringResource(R.string.nc_remove))
                                            }
                                        }
                                        
                                        else -> {

                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    item {
                        UserOnline(state.numberOfOnlineUsers)
                    }

                    var colorIndex = 0
                    itemsIndexed(state.signers) { index, signer ->
                        FreeAddKeyCard(
                            index = index,
                            isOccupied = state.occupiedSlotsIndex.contains(index),
                            signer = signer,
                            replacedSigner = state.replaceSigners.getOrNull(index),
                            onAddClicked = {
                                currentSignerIndex = index
                                onStartAddKey(index)
                                if (state.allSigners.isNotEmpty()) {
                                    showSignerBottomSheet = true
                                } else {
                                    onAddNewKey(index)
                                }
                            },
                            onRemoveOrReplaceClicked = { isReplace ->
                                currentSignerIndex = index
                                if (isReplace) {
                                    onStartAddKey(index)
                                    if (state.allSigners.isNotEmpty()) {
                                        showSignerBottomSheet = true
                                    } else {
                                        onAddNewKey(index)
                                    }
                                } else {
                                    showDeleteSignerDialog = true
                                }
                            },
                            showBip32Path = showBip32Path,
                            onChangeBip32Path = onChangeBip32Path,
                            avatarColor = if (signer?.isVisible == false) avatarColors[colorIndex++ % avatarColors.size] else avatarColors[0],
                            isInReplace = isInReplace,
                        )
                    }
                }
            }

            PullRefreshIndicator(
                state.isRefreshing,
                pullRefreshState,
                Modifier.align(Alignment.TopCenter)
            )
        }

        if (showMoreOption) {
            NcSelectableBottomSheet(
                options = listOf(
                    if (showBip32Path) stringResource(R.string.nc_hide_bip_32_path) else stringResource(
                        R.string.nc_show_bip_32_path
                    ),
                    if (isInReplace) {
                        stringResource(R.string.nc_cancel_key_replacement)
                    } else {
                        stringResource(R.string.nc_cancel_group_wallet_setup)
                    },
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

        if (showAskForDeleteDialog) {
            NcConfirmationDialog(
                message =
                    if (isInReplace) stringResource(R.string.nc_confirm_cancel_replacement_desc)
                    else stringResource(id = R.string.nc_ask_for_delete_group_wallet),
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
        
        // Key action UI components
        if (showRemoveConfirmation) {
            NcConfirmationDialog(
                title = stringResource(id = R.string.nc_text_warning),
                message = "Are you sure you want to remove the signer from this key?",
                onPositiveClick = {
                    onRemoveSignerForKey(keyToRemove)
                    showRemoveConfirmation = false
                    keyToRemove = ""
                },
                onDismiss = {
                    showRemoveConfirmation = false
                    keyToRemove = ""
                }
            )
        }
        
        if (showDuplicateSignerWarning && duplicateSignerData != null) {
            val (signer, keyName) = duplicateSignerData!!
            NcInfoDialog(
                title = stringResource(id = R.string.nc_text_warning),
                message = stringResource(id = com.nunchuk.android.core.R.string.nc_miniscript_duplicate_signer_message),
                onPositiveClick = {
                    showDuplicateSignerWarning = false
                    duplicateSignerData = null
                },
                onDismiss = {
                    showDuplicateSignerWarning = false
                    duplicateSignerData = null
                }
            )
        }
        
        if (showSignerBottomSheet) {
            val isMiniscriptMode = state.currentKeyToAssign.isNotEmpty()
            val addedSigners = if (isMiniscriptMode) {
                state.namedSigners.values.filterNotNull().map { it.fingerPrint }.toSet()
            } else {
                state.signers.filterNotNull().map { it.fingerPrint }.toSet()
            }
            
            val allSigners = state.allSigners.filter {
                !addedSigners.contains(it.fingerPrint)
            }
            
            if (allSigners.isNotEmpty()) {
                SelectSignerBottomSheet(
                    onDismiss = { showSignerBottomSheet = false },
                    supportedSigners = state.supportedTypes.takeIf { state.group?.addressType?.isTaproot() == true }
                        .orEmpty(),
                    onAddExistKey = { signer ->
                        showSignerBottomSheet = false
                        if (isMiniscriptMode) {
                            onAddExistingKeyForMiniscript(signer, state.currentKeyToAssign)
                        } else {
                            onAddExistingKey(signer, currentSignerIndex)
                        }
                    },
                    onAddNewKey = {
                        showSignerBottomSheet = false
                        if (isMiniscriptMode) {
                            onAddNewKeyForMiniscript(state.supportedTypes)
                        } else {
                            onAddNewKey(currentSignerIndex)
                        }
                    },
                    args = TapSignerListBottomSheetFragmentArgs(
                        signers = allSigners.toTypedArray(),
                        type = SignerType.UNKNOWN
                    )
                )
            } else {
                showSignerBottomSheet = false
                if (isMiniscriptMode) {
                    onAddNewKeyForMiniscript(state.supportedTypes)
                } else {
                    onAddNewKey(currentSignerIndex)
                }
            }
        }
    }
}

@Composable
private fun TaprootAddressContent(
    state: FreeGroupWalletUiState,
    showBip32Path: Boolean,
    onChangeBip32Path: (Int, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit = { _, _ -> },
    parentModifier: Modifier = Modifier
) {
    if (state.group?.addressType?.isTaproot() == true) {
        if (state.keyPath.size <= 1) {
            MiniscriptTaproot(
                keyPath = state.keyPath.firstOrNull().orEmpty(),
                data = ScriptNodeData(
                    mode = ScriptMode.CONFIG,
                    signers = state.namedSigners,
                    showBip32Path = showBip32Path,
                    occupiedSlots = state.namedOccupied
                ),
                signer = if (state.keyPath.isNotEmpty() && state.signers.isNotEmpty()) state.signers.first() else null,
                onChangeBip32Path = { keyPath, signer ->
                    val index = state.keyPath.indexOf(keyPath)
                    if (index != -1) {
                        onChangeBip32Path(index, signer)
                    }
                },
                onActionKey = onActionKey
            )
        } else if (state.scriptNodeMuSig != null) {
            NcBadgePrimary(
                modifier = Modifier.padding(vertical = 8.dp),
                text = "Key path",
                enabled = true,
            )

            Column(modifier = parentModifier) {
                val startingColorIndex = 100
                ScriptNodeTree(
                    node = state.scriptNodeMuSig,
                    data = ScriptNodeData(
                        mode = ScriptMode.CONFIG,
                        signers = state.namedSigners,
                        showBip32Path = showBip32Path,
                        isGroupWallet = true,
                        occupiedSlots = state.namedOccupied,
                        colorIndex = startingColorIndex
                    ),
                    onChangeBip32Path = { keyPath, signer ->
                        val index = state.keyPath.indexOf(keyPath)
                        if (index != -1) {
                            onChangeBip32Path(index, signer)
                        }
                    },
                    onActionKey = onActionKey,
                    customActionButton = { key, signer ->
                        Timber.tag("miniscript-feature").d("CustomActionButton (MuSig) called: key=$key, signer=${signer?.name}, isVisible=${signer?.isVisible}")
                        when {
                            signer == null -> {
                                Timber.tag("miniscript-feature").d("Showing Add button (MuSig)")
                                NcOutlineButton(
                                    height = 36.dp,
                                    onClick = { onActionKey(key, null) },
                                ) {
                                    Text(stringResource(R.string.nc_add))
                                }
                            }
                            
                            signer.isVisible -> {
                                Timber.tag("miniscript-feature").d("Showing Remove button (MuSig)")
                                NcOutlineButton(
                                    height = 36.dp,
                                    onClick = { onActionKey(key, signer) },
                                ) {
                                    Text(stringResource(R.string.nc_remove))
                                }
                            }
                            
                            else -> {
                                Timber.tag("miniscript-feature").d("No button rendered (MuSig) - signer=${signer?.name}, isVisible=${signer?.isVisible}")
                            }
                        }
                    }
                )
            }
        }
        // Add Script path badge
        NcBadgePrimary(
            modifier = Modifier.padding(
                top = 16.dp,
                bottom = 8.dp,
            ),
            text = stringResource(id = com.nunchuk.android.core.R.string.nc_miniscript_script_path),
            enabled = true
        )
    }
}

// Helper function to count total keys in a ScriptNode tree
private fun countTotalKeys(node: ScriptNode): Int {
    var count = node.keys.size
    node.subs.forEach { subNode ->
        count += countTotalKeys(subNode)
    }
    return count
}

@PreviewLightDark
@Composable
private fun GroupWalletScreenPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    val addedSigner = signers.first().copy(name = KEY_NOT_SYNCED_NAME)
    NunchukTheme {
        FreeGroupWalletScreen(
            state = FreeGroupWalletUiState(
                signers = signers + addedSigner + null,
                numberOfOnlineUsers = 2,
                group = GroupSandbox(
                    id = "group1",
                    name = "Test Group Wallet",
                    m = 2,
                    n = 3,
                    addressType = AddressType.TAPROOT,
                    walletType = WalletType.MINISCRIPT,
                    url = "https://example.com/group-wallet",
                    replaceWalletId = "",
                    miniscriptTemplate = "tr(A,{and_v(v:multi_a(2,B,C,D),older(6)),multi_a(2,B,E)})",
                    namedSigners = emptyMap(),
                    namedOccupied = emptyMap(),
                    signers = emptyList(),
                    finalized = false,
                    walletId = "",
                    occupiedSlots = emptyList()
                ),
                scriptNode = ScriptNode(
                    id = emptyList(),
                    data = byteArrayOf(),
                    type = ScriptNodeType.ANDOR.name,
                    keys = listOf(),
                    k = 0,
                    timeLock = null,
                    subs = listOf(
                        ScriptNode(
                            type = ScriptNodeType.ANDOR.name,
                            keys = listOf(),
                            subs = listOf(
                                ScriptNode(
                                    type = ScriptNodeType.ANDOR.name,
                                    keys = listOf("key_0_0", "key_1_0"),
                                    subs = emptyList(),
                                    k = 0,
                                    id = emptyList(),
                                    data = byteArrayOf(),
                                    timeLock = null
                                )
                            ),
                            k = 0,
                            id = emptyList(),
                            data = byteArrayOf(),
                            timeLock = null
                        ),
                        ScriptNode(
                            type = ScriptNodeType.SHA256.name,
                            keys = listOf("key_0_1", "key_1_1"),
                            subs = emptyList(),
                            k = 0,
                            id = emptyList(),
                            data = byteArrayOf(),
                            timeLock = null
                        )
                    )
                ),
            )
        )
    }
}

