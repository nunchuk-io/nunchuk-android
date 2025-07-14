package com.nunchuk.android.app.miniscript.configurewallet

import android.widget.Toast
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
import com.nunchuk.android.compose.miniscript.MiniscriptTaproot
import com.nunchuk.android.compose.miniscript.PolicyHeader
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.miniscript.ScripNoteType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.isTaproot
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
    val walletName: String
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
        val context = LocalContext.current

        LaunchedEffect(uiState.event) {
            when (val event = uiState.event) {
                is MiniscriptSharedWalletEvent.Loading -> {
                    // Handle loading state
                }

                null -> {}
                is MiniscriptSharedWalletEvent.Error -> {
                    Toast.makeText(
                        context,
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is MiniscriptSharedWalletEvent.SignerAdded -> {}
                is MiniscriptSharedWalletEvent.SignerRemoved -> {}
                is MiniscriptSharedWalletEvent.OpenChangeBip32Path -> {
                    onOpenChangeBip32Path(event.signer)
                }

                is MiniscriptSharedWalletEvent.Bip32PathChanged -> {
                    Toast.makeText(
                        context,
                        "BIP32 path updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is MiniscriptSharedWalletEvent.RequestCacheTapSignerXpub -> {
                    // This will be handled by the LaunchedEffect in MiniscriptActivity
                }

                is MiniscriptSharedWalletEvent.ShowDuplicateSignerWarning -> {
                    // Don't clear these events here - let the screen handle them
                    return@LaunchedEffect
                }

                is MiniscriptSharedWalletEvent.ShowDuplicateSignerUpdateWarning -> {
                    // Don't clear these events here - let the screen handle them
                    return@LaunchedEffect
                }

                else -> {}
            }
            viewModel.onEventHandled()
        }

        // Load signers when the screen returns from adding a new key
        LifecycleResumeEffect(uiState.currentKeyToAssign) {
            if (uiState.currentKeyToAssign.isNotEmpty()) {
                viewModel.checkForNewlyAddedSigner()
            }
            onPauseOrDispose { }
        }

        MiniscriptConfigWalletScreen(
            uiState = uiState,
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
    onProceedWithDuplicateSigner: (SignerModel, String) -> Unit = { _, _ -> },
    onProceedWithDuplicateBip32Update: () -> Unit = {},
    onClearEvent: () -> Unit = {}
) {
    var showSignerBottomSheet by rememberSaveable { mutableStateOf(false) }
    var currentKeyToAssign by rememberSaveable { mutableStateOf("") }
    var showMoreOption by rememberSaveable { mutableStateOf(false) }
    var showBip32Path by rememberSaveable { mutableStateOf(uiState.showBip32PathForDuplicates) }
    var showDuplicateSignerWarning by rememberSaveable { mutableStateOf(false) }
    var duplicateSignerData by rememberSaveable { mutableStateOf<Pair<SignerModel, String>?>(null) }
    var isDuplicateBip32Update by rememberSaveable { mutableStateOf(false) }
    var showRemoveConfirmation by rememberSaveable { mutableStateOf(false) }
    var keyToRemove by rememberSaveable { mutableStateOf("") }

    // Handle duplicate signer warning events
    LaunchedEffect(uiState.event) {
        when (uiState.event) {
            is MiniscriptSharedWalletEvent.ShowDuplicateSignerWarning -> {
                duplicateSignerData = Pair(uiState.event.signer, uiState.event.keyName)
                isDuplicateBip32Update = false
                showDuplicateSignerWarning = true
            }

            is MiniscriptSharedWalletEvent.ShowDuplicateSignerUpdateWarning -> {
                duplicateSignerData = Pair(uiState.event.signer, uiState.event.keyName)
                isDuplicateBip32Update = true
                showDuplicateSignerWarning = true
            }

            else -> {}
        }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "Configure wallet",
                    textStyle = NunchukTheme.typography.titleLarge,
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val hasDuplicateSigners = getDuplicateSignerKeys(uiState.signers, uiState.taprootSigner).isNotEmpty()
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onContinue() },
                        enabled = uiState.areAllKeysAssigned && !hasDuplicateSigners
                    ) {
                        Text(text = "Continue")
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
                PolicyHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )

                // Add MiniscriptTaproot component if addressType is TAPROOT
                if (uiState.addressType == AddressType.TAPROOT) {
                    MiniscriptTaproot(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        keyPath = uiState.keyPath,
                        data = ScriptNodeData(
                            mode = ScriptMode.CONFIG,
                            signers = uiState.signers,
                            showBip32Path = showBip32Path,
                            duplicateSignerKeys = if (uiState.showBip32PathForDuplicates) {
                                getDuplicateSignerKeys(uiState.signers, uiState.taprootSigner)
                            } else emptySet()
                        ),
                        signer = if (uiState.keyPath.isNotEmpty()) uiState.taprootSigner else null,
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
                    )

                    // Add Script path badge
                    NcBadgePrimary(
                        modifier = Modifier.padding(
                            top = 16.dp,
                            bottom = 8.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        text = "Script path",
                        enabled = true
                    )
                }

                val parentModifier = Modifier.padding(horizontal = 16.dp)

                Column(modifier = parentModifier) {
                    uiState.scriptNode?.let { scriptNode ->
                        ScriptNodeTree(
                            node = scriptNode,
                            index = if (uiState.addressType == AddressType.TAPROOT && uiState.keyPath.isNotEmpty()) "2" else "1",
                            data = ScriptNodeData(
                                mode = ScriptMode.CONFIG,
                                signers = uiState.signers,
                                showBip32Path = showBip32Path,
                                duplicateSignerKeys = if (uiState.showBip32PathForDuplicates) {
                                    getDuplicateSignerKeys(uiState.signers, uiState.taprootSigner)
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
                    supportedSigners = uiState.supportedTypes.takeIf { uiState.addressType.isTaproot() == true }
                        .orEmpty(),
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
                title = "Warning",
                message = "This key is already used in this miniscript. Select a different BIP-32 path to derive a new child key, or add it in a separate miniscript instead.",
                onPositiveClick = {
                    showDuplicateSignerWarning = false
                    val (signer, keyName) = duplicateSignerData!!

                    // Check if this is a BIP32 update warning or a regular duplicate signer warning
                    if (isDuplicateBip32Update) {
                        // For BIP32 path update duplicates, call the new function
                        onProceedWithDuplicateBip32Update()
                    } else {
                        // For regular duplicate signer warnings, use the existing function
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
                positiveButtonText = "Show BIP32 path",
                negativeButtonText = "Cancel"
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
    }
}

private fun getDuplicateSignerKeys(
    signers: Map<String, SignerModel?>,
    taprootSigner: SignerModel?
): Set<String> {
    val signerKeyCounts = mutableMapOf<String, Int>()

    // Create a unique key for each signer combining fingerprint and derivation path
    signers.values.filterNotNull().forEach { signer ->
        val signerKey = "${signer.fingerPrint}:${signer.derivationPath}"
        signerKeyCounts[signerKey] = signerKeyCounts.getOrDefault(signerKey, 0) + 1
    }

    // Count taproot signer
    taprootSigner?.let { signer ->
        val signerKey = "${signer.fingerPrint}:${signer.derivationPath}"
        signerKeyCounts[signerKey] = signerKeyCounts.getOrDefault(signerKey, 0) + 1
    }

    // Return signer keys that appear more than once
    return signerKeyCounts.filter { it.value > 1 }.keys.toSet()
}

@PreviewLightDark
@Composable
fun MiniscriptConfigWalletScreenPreview() {
    val previewState = MiniscriptSharedWalletState(
        scriptNode = ScriptNode(
            id = emptyList(),
            data = byteArrayOf(),
            type = ScripNoteType.ANDOR.name,
            keys = listOf(),
            k = 0,
            timeLock = null,
            subs = listOf(
                ScriptNode(
                    type = ScripNoteType.ANDOR.name,
                    keys = listOf(),
                    subs = listOf(
                        ScriptNode(
                            type = ScripNoteType.ANDOR.name,
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
                    type = ScripNoteType.SHA256.name,
                    keys = listOf("key_0_1", "key_1_1"),
                    subs = emptyList(),
                    k = 0,
                    id = emptyList(),
                    data = byteArrayOf(),
                    timeLock = null
                )
            )
        ),
        keyPath = "key_taproot",
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