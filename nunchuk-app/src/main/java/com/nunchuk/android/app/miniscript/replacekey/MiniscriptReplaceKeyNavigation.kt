/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.app.miniscript.replacekey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.compose.NcBadgePrimary
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.miniscript.EditTimelockBottomSheet
import com.nunchuk.android.compose.miniscript.MiniscriptTaproot
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.compose.miniscript.TimelockData
import com.nunchuk.android.compose.provider.ScriptNodeProvider
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.list.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.share.miniscript.rememberBlockHeightManager
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.type.MiniscriptTimelockType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletTemplate
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data class MiniscriptReplaceKeyRoute(
    val walletId: String
)

fun NavGraphBuilder.miniscriptReplaceKeyDestination(
    onCreateWalletSuccess: (Wallet) -> Unit = {}
) {
    composable<MiniscriptReplaceKeyRoute> { navBackStackEntry ->
        val route: MiniscriptReplaceKeyRoute = navBackStackEntry.toRoute()
        MiniscriptReplaceKeyScreen(
            onCreateWalletSuccess = onCreateWalletSuccess,
        )
    }
}

@Composable
fun MiniscriptReplaceKeyScreen(
    viewModel: MiniscriptReplaceKeyViewModel = hiltViewModel(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onCreateWalletSuccess: (Wallet) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(event) {
        event?.let {
            when (it) {
                is MiniscriptReplaceKeyEvent.Error -> {
                    snackState.showSnackbar(it.message)
                }
                is MiniscriptReplaceKeyEvent.CreateWalletSuccess -> {
                    onCreateWalletSuccess(it.wallet)
                }
            }
        }
    }

    MiniscriptReplaceKeyContent(
        state = state,
        snackState = snackState,
        onContinueClicked = viewModel::createMiniscriptWallet,
        onUpdateTimelockReplaceData = viewModel::updateTimelockReplaceData,
        onSetCurrentKeyToReplace = viewModel::setCurrentKeyToReplace,
        onReplaceSignerWithExisting = viewModel::replaceSignerWithExisting,
        onRemoveReplacedSigner = viewModel::removeReplacedSigner,
        onRemoveReplacedTimelock = viewModel::removeReplacedTimelock
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniscriptReplaceKeyContent(
    state: MiniscriptReplaceKeyState = MiniscriptReplaceKeyState(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onContinueClicked: () -> Unit = {},
    onUpdateTimelockReplaceData: (originalNode: ScriptNode, timelockData: TimelockData) -> Unit = { _, _ -> },
    onSetCurrentKeyToReplace: (String) -> Unit = {},
    onReplaceSignerWithExisting: (SignerModel, String) -> Unit = { _, _ -> },
    onRemoveReplacedSigner: (String) -> Unit = {},
    onRemoveReplacedTimelock: (String) -> Unit = {}
) = NunchukTheme {
    var showTimelockBottomSheet by remember { mutableStateOf(false) }
    var selectedTimelockNode by remember { mutableStateOf<ScriptNode?>(null) }
    val timelockSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Get current block height
    val blockHeightManager = rememberBlockHeightManager()
    val currentBlockHeight by blockHeightManager.state.collectAsStateWithLifecycle()
    
    var showSignerBottomSheet by rememberSaveable { mutableStateOf(false) }
    var currentKeyToReplace by rememberSaveable { mutableStateOf("") }
    var showRemoveConfirmation by rememberSaveable { mutableStateOf(false) }
    var keyToRemove by rememberSaveable { mutableStateOf("") }
    var showRemoveTimelockConfirmation by rememberSaveable { mutableStateOf(false) }
    var timelockNodeToRemove by remember { mutableStateOf<ScriptNode?>(null) }
    
    if (state.isLoading) {
        NcLoadingDialog()
    }

    NcScaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcTopAppBar(
                title = "",
                textStyle = NunchukTheme.typography.titleLarge,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked,
                enabled = state.scriptNode != null && state.signerMap.isNotEmpty()
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        if (state.scriptNode != null && state.signerMap.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = "Replace key or change timelock",
                        style = NunchukTheme.typography.heading
                    )
                }
                item {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 24.dp
                        )
                    ) {
                        TaprootAddressContent(
                            state = state,
                            onChangeTimelockButton = { node ->
                                val replaceData = state.replaceKeyData[node.idString]
                                if (replaceData?.replacedTimeLock != null) {
                                    // Has replacement - show remove confirmation
                                    timelockNodeToRemove = node
                                    showRemoveTimelockConfirmation = true
                                } else {
                                    // No replacement - show edit timelock sheet
                                    selectedTimelockNode = node
                                    showTimelockBottomSheet = true
                                }
                            },
                            onActionKey = { keyName, signer ->
                                val replaceData = state.replaceKeyData[keyName]
                                if (replaceData?.replacedSigner != null) {
                                    // Has replacement - show remove confirmation
                                    keyToRemove = keyName
                                    showRemoveConfirmation = true
                                } else {
                                    // No replacement - show signer selection
                                    currentKeyToReplace = keyName
                                    onSetCurrentKeyToReplace(keyName)
                                    showSignerBottomSheet = true
                                }
                            }
                        )

                        ScriptNodeTree(
                            node = state.scriptNode,
                            data = ScriptNodeData(
                                mode = ScriptMode.CHANGE_KEY,
                                signers = state.signerMap,
                                showBip32Path = true,
                                replaceKeyData = state.replaceKeyData
                            ),
                            onChangeBip32Path = { _, _ -> },
                            onActionKey = { keyName, signer ->
                                val replaceData = state.replaceKeyData[keyName]
                                if (replaceData?.replacedSigner != null) {
                                    // Has replacement - show remove confirmation
                                    keyToRemove = keyName
                                    showRemoveConfirmation = true
                                } else {
                                    // No replacement - show signer selection
                                    currentKeyToReplace = keyName
                                    onSetCurrentKeyToReplace(keyName)
                                    showSignerBottomSheet = true
                                }
                            },
                            customActionButton = { key, signer ->
                                val replaceData = state.replaceKeyData[key]
                                val hasReplacement = replaceData?.replacedSigner != null
                                NcOutlineButton(
                                    height = 36.dp,
                                    onClick = {
                                        if (hasReplacement) {
                                            keyToRemove = key
                                            showRemoveConfirmation = true
                                        } else {
                                            currentKeyToReplace = key
                                            onSetCurrentKeyToReplace(key)
                                            showSignerBottomSheet = true
                                        }
                                    },
                                ) {
                                    Text(
                                        text = if (hasReplacement) {
                                            stringResource(R.string.nc_remove)
                                        } else {
                                            stringResource(R.string.nc_change)
                                        }
                                    )
                                }
                            },
                            onChangeTimelockButton = { node ->
                                val replaceData = state.replaceKeyData[node.idString]
                                if (replaceData?.replacedTimeLock != null) {
                                    // Has replacement - show remove confirmation
                                    timelockNodeToRemove = node
                                    showRemoveTimelockConfirmation = true
                                } else {
                                    // No replacement - show edit timelock sheet
                                    selectedTimelockNode = node
                                    showTimelockBottomSheet = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showTimelockBottomSheet && selectedTimelockNode != null) {
        val node = selectedTimelockNode!!
        // Determine timelock type (ABSOLUTE/RELATIVE) from node type
        val timelockType = when (node.type) {
            ScriptNodeType.AFTER.name -> MiniscriptTimelockType.ABSOLUTE
            ScriptNodeType.OLDER.name -> MiniscriptTimelockType.RELATIVE
            else -> MiniscriptTimelockType.ABSOLUTE
        }
        // Determine time unit from node.timeLock.based
        val timeUnit = node.timeLock?.based ?: MiniscriptTimelockBased.TIME_LOCK
        val initialTimelockValue = node.timeLock?.value ?: 0L
        
        // Create initial data from the node's timelock
        val initialData = TimelockData(
            timelockType = timelockType,
            timeUnit = timeUnit,
            value = initialTimelockValue
        )
        
        EditTimelockBottomSheet(
            sheetState = timelockSheetState,
            currentBlockHeight = currentBlockHeight.toLong(),
            initialData = initialData,
            snackbarHostState = snackState,
            onDismiss = { showTimelockBottomSheet = false },
            onSave = { timelockData ->
                Timber.tag("miniscript-feature").d("Timelock saved: $timelockData for node ${node.idString}")
                onUpdateTimelockReplaceData(node, timelockData)
                showTimelockBottomSheet = false
            }
        )
    }
    
    if (showSignerBottomSheet) {
        val allSigners = state.allSigners
        if (allSigners.isNotEmpty()) {
            SelectSignerBottomSheet(
                onDismiss = {
                    showSignerBottomSheet = false
                },
                supportedSigners = state.supportedTypes,
                onAddExistKey = { signer ->
                    showSignerBottomSheet = false
                    onReplaceSignerWithExisting(signer, currentKeyToReplace)
                },
                onAddNewKey = {
                    showSignerBottomSheet = false
                    // TODO: Handle add new key if needed
                },
                args = TapSignerListBottomSheetFragmentArgs(
                    signers = allSigners.toTypedArray(),
                    type = SignerType.UNKNOWN
                )
            )
        }
    }
    
    if (showRemoveConfirmation) {
        NcConfirmationDialog(
            title = stringResource(id = R.string.nc_text_warning),
            message = stringResource(id = R.string.nc_ask_for_delete_signer),
            onPositiveClick = {
                showRemoveConfirmation = false
                onRemoveReplacedSigner(keyToRemove)
                keyToRemove = ""
            },
            onDismiss = {
                showRemoveConfirmation = false
                keyToRemove = ""
            }
        )
    }
    
    if (showRemoveTimelockConfirmation && timelockNodeToRemove != null) {
        NcConfirmationDialog(
            title = stringResource(id = R.string.nc_text_warning),
            message = "Are you sure you want to remove this timelock change?",
            onPositiveClick = {
                showRemoveTimelockConfirmation = false
                onRemoveReplacedTimelock(timelockNodeToRemove!!.idString)
                timelockNodeToRemove = null
            },
            onDismiss = {
                showRemoveTimelockConfirmation = false
                timelockNodeToRemove = null
            }
        )
    }
}

@Composable
private fun TaprootAddressContent(
    state: MiniscriptReplaceKeyState,
    onChangeTimelockButton: (ScriptNode) -> Unit = { },
    onActionKey: (String, SignerModel?) -> Unit = { _, _ -> }
) {
    val wallet = state.walletExtended.wallet
    if (wallet.addressType == AddressType.TAPROOT) {
        val scriptNodeMuSig = state.scriptNodeMuSig
        if (scriptNodeMuSig != null) {
            NcBadgePrimary(
                modifier = Modifier.padding(
                    vertical = 8.dp
                ),
                text = "Key path",
                enabled = true,
            )

            ScriptNodeTree(
                node = scriptNodeMuSig,
                data = ScriptNodeData(
                    mode = ScriptMode.CHANGE_KEY,
                    signers = state.muSigSignerMap,
                    showBip32Path = true,
                    replaceKeyData = state.replaceKeyData
                ),
                onChangeBip32Path = { _, _ -> },
                onActionKey = onActionKey,
                customActionButton = { key, signer ->
                    val replaceData = state.replaceKeyData[key]
                    val hasReplacement = replaceData?.replacedSigner != null
                    NcOutlineButton(
                        height = 36.dp,
                        onClick = { onActionKey(key, signer) },
                    ) {
                        Text(
                            text = if (hasReplacement) {
                                stringResource(R.string.nc_remove)
                            } else {
                                stringResource(R.string.nc_change)
                            }
                        )
                    }
                },
                onChangeTimelockButton = onChangeTimelockButton
            )
        } else {
            val keyPath =
                if (wallet.walletTemplate == WalletTemplate.DISABLE_KEY_PATH) {
                    ""
                } else {
                    wallet.signers.firstOrNull()?.name
                }
            MiniscriptTaproot(
                keyPath = keyPath.orEmpty(),
                data = ScriptNodeData(
                    mode = ScriptMode.CHANGE_KEY,
                    signers = state.signerMap,
                    showBip32Path = true,
                    replaceKeyData = state.replaceKeyData
                ),
                signer = wallet.signers.firstOrNull()?.toModel(),
                onChangeBip32Path = { _, _ -> },
                onActionKey = onActionKey
            )
        }

        // Add Script path badge
        NcBadgePrimary(
            modifier = Modifier.padding(
                top = 16.dp,
                bottom = 8.dp,
                end = 16.dp
            ),
            text = "Script path",
            enabled = true
        )
    }
}

@Preview
@Composable
fun MiniscriptReplaceKeyScreenPreview() {
    MiniscriptReplaceKeyContent(
        state = MiniscriptReplaceKeyState(
            walletExtended = WalletExtended(
                wallet = Wallet(
                    name = "Test Wallet",
                    addressType = AddressType.TAPROOT,
                    miniscript = "thresh(2,pk(key1),pk(key2),pk(key3))"
                )
            ),
            scriptNode = ScriptNodeProvider().values.first(),
            signerMap = emptyMap()
        )
    )
}

