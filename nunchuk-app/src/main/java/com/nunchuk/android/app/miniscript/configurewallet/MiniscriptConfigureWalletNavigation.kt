package com.nunchuk.android.app.miniscript.configurewallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.app.miniscript.MiniscriptSharedWalletEvent
import com.nunchuk.android.app.miniscript.MiniscriptSharedWalletState
import com.nunchuk.android.app.miniscript.MiniscriptSharedWalletViewModel
import com.nunchuk.android.compose.NcBadgePrimary
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcConfirmationVerticalDialog
import com.nunchuk.android.compose.dialog.NcInputDialog
import com.nunchuk.android.compose.dialog.NcInputType
import com.nunchuk.android.compose.miniscript.MiniscriptTaproot
import com.nunchuk.android.compose.miniscript.PolicyHeader
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.list.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data class MiniscriptConfigureWallet(
    val template: String,
    val addressType: AddressType,
    val walletName: String,
    val reuseSigner: Boolean = false
)

fun NavGraphBuilder.miniscriptConfigureWalletDestination(
    viewModel: MiniscriptSharedWalletViewModel,
    onAddNewKey: (List<SupportedSigner>) -> Unit = {},
    onContinue: () -> Unit = { },
    onOpenChangeBip32Path: (SignerModel) -> Unit = {}
) {
    composable<MiniscriptConfigureWallet> { navBackStackEntry ->
        val data: MiniscriptConfigureWallet = navBackStackEntry.toRoute()

        LaunchedEffect(data) {
            // Clear and reset state to create fresh instance effect
            viewModel.clearAndResetState()
            viewModel.init(args = data)
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Load signers when the screen returns from adding a new key
        LifecycleResumeEffect(uiState.currentKeyToAssign) {
            if (uiState.currentKeyToAssign.isNotEmpty()) {
                viewModel.checkForNewlyAddedSigner()
            }
            onPauseOrDispose { }
        }

        MiniscriptConfigWalletScreen(
            uiState = uiState,
            onOpenChangeBip32Path = onOpenChangeBip32Path,
            onAddNewKey = {
                onAddNewKey(viewModel.getSuggestedSigners())
            },
            onAddExistingKey = { signer, keyName ->
                viewModel.addExistingSigner(signer, keyName)
            },
            onSetCurrentKey = { keyName ->
                viewModel.setCurrentKeyToAssign(keyName)
            },
            onRemoveClicked = { keyName ->
                viewModel.removeSigner(keyName)
            },
            onContinue = {
                uiState.scriptNode?.let { scriptNode ->
                    onContinue()
                }
            },
            onChangeBip32Path = { keyName, signer ->
                viewModel.changeBip32Path(keyName, signer)
            },
            onProceedWithDuplicateSigner = { signer, keyName ->
                viewModel.proceedWithDuplicateSigner(signer, keyName)
            },
            onProceedWithDuplicateBip32Update = {
                viewModel.proceedWithDuplicateBip32Update()
            },
            onSubmitPassphrase = { passphrase ->
                viewModel.submitPassphrase(passphrase)
            },
            onClearEvent = {
                viewModel.onEventHandled()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniscriptConfigWalletScreen(
    uiState: MiniscriptSharedWalletState = MiniscriptSharedWalletState(),
    onAddNewKey: () -> Unit = {},
    onRemoveClicked: (String) -> Unit = {},
    onAddExistingKey: (SignerModel, String) -> Unit = { _, _ -> },
    onSetCurrentKey: (String) -> Unit = {},
    onContinue: () -> Unit = {},
    onChangeBip32Path: (String, SignerModel) -> Unit = { _, _ -> },
    onOpenChangeBip32Path: (SignerModel) -> Unit = {},
    onProceedWithDuplicateSigner: (SignerModel, String) -> Unit = { _, _ -> },
    onProceedWithDuplicateBip32Update: () -> Unit = {},
    onSubmitPassphrase: (String) -> Unit = {},
    onClearEvent: () -> Unit = {}
) {
    var showSignerBottomSheet by rememberSaveable { mutableStateOf(false) }
    var currentKeyToAssign by rememberSaveable { mutableStateOf("") }
    var showMoreOption by rememberSaveable { mutableStateOf(false) }
    var showBip32Path by rememberSaveable(uiState.reuseSigner) { mutableStateOf(uiState.reuseSigner) }
    var showDuplicateSignerWarning by rememberSaveable { mutableStateOf(false) }
    var duplicateSignerData by rememberSaveable { mutableStateOf<Pair<SignerModel, String>?>(null) }
    var isDuplicateBip32Update by rememberSaveable { mutableStateOf(false) }
    var showRemoveConfirmation by rememberSaveable { mutableStateOf(false) }
    var keyToRemove by rememberSaveable { mutableStateOf("") }
    var showPassphraseDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    
    // Consolidated event handling for all MiniscriptSharedWalletEvent types
    LaunchedEffect(uiState.event) {
        Timber.tag("miniscript-feature").d("Screen level - UI OBSERVED event change: ${uiState.event}")
        Timber.tag("miniscript-feature").d("Screen level - MiniscriptConfigWalletScreen LaunchedEffect triggered with event: ${uiState.event}")
        when (val event = uiState.event) {
            is MiniscriptSharedWalletEvent.Error -> {
                android.widget.Toast.makeText(
                    context,
                    event.message,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            is MiniscriptSharedWalletEvent.SignerAdded -> {
                // UI will react automatically via uiState changes
            }
            
            is MiniscriptSharedWalletEvent.SignerRemoved -> {
                // UI will react automatically via uiState changes
            }
            
            is MiniscriptSharedWalletEvent.OpenChangeBip32Path -> {
                onOpenChangeBip32Path(event.signer)
            }
            
            is MiniscriptSharedWalletEvent.Bip32PathChanged -> {
                android.widget.Toast.makeText(
                    context,
                    "BIP32 path updated successfully",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            is MiniscriptSharedWalletEvent.RequestCacheTapSignerXpub -> {
                return@LaunchedEffect
            }
            
            is MiniscriptSharedWalletEvent.ShowDuplicateSignerWarning -> {
                duplicateSignerData = Pair(event.signer, event.keyName)
                isDuplicateBip32Update = false
                showDuplicateSignerWarning = true
            }
            
            is MiniscriptSharedWalletEvent.ShowDuplicateSignerUpdateWarning -> {
                duplicateSignerData = Pair(event.signer, event.keyName)
                isDuplicateBip32Update = true
                showDuplicateSignerWarning = true
            }
            
            is MiniscriptSharedWalletEvent.RequestPassphrase -> {
                showPassphraseDialog = true
            }

            is MiniscriptSharedWalletEvent.CreateWalletSuccess -> {
                // UI will handle this automatically through navigation
            }
            
            else -> {}
        }
        
        onClearEvent()
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_miniscript_configure_wallet),
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.textPrimary) {
                            IconButton(onClick = {
                                showMoreOption = true
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val hasDuplicateSigners =
                        getDuplicateSignerKeys(uiState.signers).isNotEmpty()
                    val isContinueEnabled = uiState.areAllKeysAssigned && !hasDuplicateSigners

                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onContinue() },
                        enabled = isContinueEnabled
                    ) {
                        Text(text = stringResource(id = R.string.nc_miniscript_continue))
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                val parentModifier = Modifier.padding(horizontal = 16.dp)

                PolicyHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )

                TaprootAddressContent(
                    uiState = uiState,
                    showBip32Path = showBip32Path,
                    onChangeBip32Path = onChangeBip32Path,
                    onActionKey = { keyName, signer ->
                        if (signer != null) {
                            keyToRemove = keyName
                            showRemoveConfirmation = true
                        } else {
                            Timber.tag("miniscript-feature").e("Adding new key: $keyName")
                            currentKeyToAssign = keyName
                            onSetCurrentKey(keyName)
                            showSignerBottomSheet = true
                        }
                    },
                    parentModifier = parentModifier
                )

                Column(modifier = parentModifier) {
                    uiState.scriptNode?.let { scriptNode ->
                        ScriptNodeTree(
                            node = scriptNode,
                            data = ScriptNodeData(
                                mode = ScriptMode.CONFIG,
                                signers = uiState.signers,
                                showBip32Path = showBip32Path,
                                duplicateSignerKeys = if (uiState.showBip32PathForDuplicates) {
                                    getDuplicateSignerKeys(uiState.signers)
                                } else emptySet()
                            ),
                            onChangeBip32Path = onChangeBip32Path,
                            onActionKey = { keyName, signer ->
                                if (signer != null) {
                                    keyToRemove = keyName
                                    showRemoveConfirmation = true
                                } else {
                                    Timber.tag("miniscript-feature").e("Adding new key: $keyName")
                                    currentKeyToAssign = keyName
                                    onSetCurrentKey(keyName)
                                    showSignerBottomSheet = true
                                }
                            }
                            // Using default behavior for this screen, no custom action button needed
                        )
                    }
                }
            }
        }

        if (showSignerBottomSheet) {
            val allSigners = uiState.allSigners
            if (allSigners.isNotEmpty()) {
                SelectSignerBottomSheet(
                    onDismiss = {
                        showSignerBottomSheet = false
                    },
                    supportedSigners = uiState.supportedTypes,
                    onAddExistKey = { signer ->
                        showSignerBottomSheet = false
                        onAddExistingKey(signer, currentKeyToAssign)
                    },
                    onAddNewKey = {
                        showSignerBottomSheet = false
                        onAddNewKey()
                        // Keep currentKeyToAssign until we return from adding a new key
                    },
                    args = TapSignerListBottomSheetFragmentArgs(
                        signers = allSigners.toTypedArray(),
                        type = SignerType.UNKNOWN
                    )
                )
            } else {
                showSignerBottomSheet = false
                onAddNewKey()
            }
        }

        if (showMoreOption) {
            NcSelectableBottomSheet(
                options = listOf(
                    if (showBip32Path) stringResource(R.string.nc_hide_bip_32_path) else stringResource(
                        R.string.nc_show_bip_32_path
                    )
                ),
                onSelected = {
                    showBip32Path = !showBip32Path
                },
                onDismiss = {
                    showMoreOption = false
                },
            )
        }

        if (showDuplicateSignerWarning && duplicateSignerData != null) {
            NcConfirmationVerticalDialog(
                title = stringResource(id = R.string.nc_miniscript_warning),
                message = stringResource(id = R.string.nc_miniscript_duplicate_signer_message),
                onPositiveClick = {
                    showDuplicateSignerWarning = false
                    val (signer, keyName) = duplicateSignerData!!

                    if (isDuplicateBip32Update) {
                        onProceedWithDuplicateBip32Update()
                    } else {
                        onProceedWithDuplicateSigner(signer, keyName)
                    }

                    duplicateSignerData = null
                    isDuplicateBip32Update = false
                    showBip32Path = true
                    onClearEvent()
                },
                onDismiss = {
                    showDuplicateSignerWarning = false
                    duplicateSignerData = null
                    isDuplicateBip32Update = false
                    onClearEvent()
                },
                positiveButtonText = stringResource(id = R.string.nc_miniscript_show_bip32_path),
                negativeButtonText = stringResource(id = R.string.nc_miniscript_cancel)
            )
        }

        if (showRemoveConfirmation) {
            NcConfirmationDialog(
                title = stringResource(id = R.string.nc_text_warning),
                message = stringResource(id = R.string.nc_ask_for_delete_signer),
                onPositiveClick = {
                    showRemoveConfirmation = false
                    onRemoveClicked(keyToRemove)
                    keyToRemove = ""
                },
                onDismiss = {
                    showRemoveConfirmation = false
                    keyToRemove = ""
                }
            )
        }

        if (showPassphraseDialog) {
            Timber.tag("miniscript-feature").d("Composing NcInputDialog for passphrase input")
            NcInputDialog(
                title = stringResource(id = R.string.nc_transaction_enter_passphrase),
                inputType = NcInputType.PASSWORD,
                isMaskedInput = true,
                onConfirmed = { passphrase ->
                    Timber.tag("miniscript-feature").d("NcInputDialog onConfirmed called with passphrase length: ${passphrase.length}")
                    showPassphraseDialog = false
                    onSubmitPassphrase(passphrase)
                    onClearEvent()
                },
                onCanceled = {
                    Timber.tag("miniscript-feature").d("NcInputDialog onCanceled called")
                    showPassphraseDialog = false
                    onClearEvent()
                },
                onDismiss = {
                    Timber.tag("miniscript-feature").d("NcInputDialog onDismiss called")
                    showPassphraseDialog = false
                    onClearEvent()
                }
            )
        }
    }
}

@Composable
private fun TaprootAddressContent(
    uiState: MiniscriptSharedWalletState,
    showBip32Path: Boolean,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    parentModifier: Modifier = Modifier
) {
    if (uiState.addressType == AddressType.TAPROOT) {
        if (uiState.keyPaths.size <= 1) {
            MiniscriptTaproot(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                keyPath = uiState.keyPaths.firstOrNull().orEmpty(),
                data = ScriptNodeData(
                    mode = ScriptMode.CONFIG,
                    signers = uiState.signers,
                    showBip32Path = showBip32Path,
                    duplicateSignerKeys = if (uiState.showBip32PathForDuplicates) {
                        getDuplicateSignerKeys(uiState.signers)
                    } else emptySet()
                ),
                signer = if (uiState.keyPaths.isNotEmpty()) uiState.signers[uiState.keyPaths.first()] else null,
                onChangeBip32Path = onChangeBip32Path,
                onActionKey = onActionKey
            )
        } else if (uiState.scriptNodeMuSig != null) {
            NcBadgePrimary(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = "Key path",
                enabled = true,
            )

            Column(modifier = parentModifier) {
                ScriptNodeTree(
                    node = uiState.scriptNodeMuSig,
                    data = ScriptNodeData(
                        mode = ScriptMode.CONFIG,
                        signers = uiState.signers,
                        showBip32Path = showBip32Path,
                        duplicateSignerKeys = if (uiState.showBip32PathForDuplicates) {
                            getDuplicateSignerKeys(uiState.signers)
                        } else emptySet()
                    ),
                    onChangeBip32Path = onChangeBip32Path,
                    onActionKey = onActionKey
                )
            }
        }

        // Add Script path badge
        NcBadgePrimary(
            modifier = Modifier.padding(
                top = 16.dp,
                bottom = 8.dp,
                start = 16.dp,
                end = 16.dp
            ),
            text = stringResource(id = R.string.nc_miniscript_script_path),
            enabled = true
        )
    }
}

private fun getDuplicateSignerKeys(
    signers: Map<String, SignerModel?>
): Set<String> {
    val signerKeyCounts = mutableMapOf<String, Int>()
    val duplicateKeys = mutableSetOf<String>()

    // Create a unique key for each signer combining fingerprint and derivation path
    signers.values.filterNotNull().forEach { signer ->
        val signerKey = "${signer.fingerPrint}:${signer.derivationPath}"
        val currentCount = signerKeyCounts.getOrDefault(signerKey, 0)
        signerKeyCounts[signerKey] = currentCount + 1

        Timber.tag("miniscript-feature")
            .d("  - Signer: $signerKey (count: ${currentCount + 1})")

        if (currentCount > 0) {
            duplicateKeys.add(signerKey)
            Timber.tag("miniscript-feature").d("  - DUPLICATE FOUND: $signerKey")
        }
    }

    // Return signer keys that appear more than once
    val result = signerKeyCounts.filter { it.value > 1 }.keys.toSet()
    Timber.tag("miniscript-feature").d("getDuplicateSignerKeys - Final result: $result")

    return result
}

@PreviewLightDark
@Composable
fun MiniscriptConfigWalletScreenPreview() {
    val previewState = MiniscriptSharedWalletState(
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
        keyPaths = listOf("key_taproot"),
        addressType = AddressType.TAPROOT,
        signers = mapOf(
            "key_0_0" to null,
            "key_1_0" to null,
            "key_0_1" to null,
            "key_1_1" to null,
            "key_taproot" to null
        ),
        areAllKeysAssigned = false
    )

    NunchukTheme {
        MiniscriptConfigWalletScreen(
            uiState = previewState
        )
    }
}