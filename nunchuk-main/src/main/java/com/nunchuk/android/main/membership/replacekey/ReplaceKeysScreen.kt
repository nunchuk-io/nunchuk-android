package com.nunchuk.android.main.membership.replacekey

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.fillSlimeT2
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.honey.distribution.InheritanceClaimStatusRow
import com.nunchuk.android.main.membership.model.InheritanceClaimState
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

@Composable
fun ReplaceKeysScreen(
    viewModel: ReplaceKeysViewModel = hiltViewModel(),
    onReplaceKeyClicked: (SignerModel) -> Unit = {},
    onReplaceInheritanceClicked: (SignerModel) -> Unit = {},
    onCreateNewWalletSuccess: (String) -> Unit = {},
    onVerifyClicked: (SignerModel) -> Unit = {},
    onSetUpClaimOptionsClicked: (SignerModel) -> Unit = {},
    onInheritanceBackupClicked: (SignerModel) -> Unit = {},
    onRemove: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.createWalletSuccess) {
        if (uiState.createWalletSuccess is StateEvent.String) {
            onCreateNewWalletSuccess((uiState.createWalletSuccess as StateEvent.String).data)
            viewModel.markOnCreateWalletSuccess()
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            snackState.showNunchukSnackbar(message = uiState.message, type = NcToastType.ERROR)
            viewModel.onHandledMessage()
        }
    }

    ReplaceKeysContent(
        uiState = uiState,
        isEnableCreateWallet = viewModel.isEnableContinueButton(),
        snackState = snackState,
        onReplaceKeyClicked = onReplaceKeyClicked,
        onReplaceInheritanceClicked = onReplaceInheritanceClicked,
        onCreateWalletClicked = viewModel::onCreateWallet,
        onCancelReplaceWallet = viewModel::onCancelReplaceWallet,
        onVerifyClicked = onVerifyClicked,
        onSetUpClaimOptionsClicked = onSetUpClaimOptionsClicked,
        onInheritanceBackupClicked = onInheritanceBackupClicked,
        onRemove = onRemove
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplaceKeysContent(
    uiState: ReplaceKeysUiState = ReplaceKeysUiState(),
    isEnableCreateWallet: Boolean = false,
    onReplaceKeyClicked: (SignerModel) -> Unit = {},
    onReplaceInheritanceClicked: (SignerModel) -> Unit = {},
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onCreateWalletClicked: () -> Unit = {},
    onCancelReplaceWallet: () -> Unit = {},
    onVerifyClicked: (SignerModel) -> Unit = {},
    onSetUpClaimOptionsClicked: (SignerModel) -> Unit = {},
    onInheritanceBackupClicked: (SignerModel) -> Unit = {},
    onRemove: (String) -> Unit = {}
) {
    var showSheetOptions by rememberSaveable { mutableStateOf(false) }
    var showConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var selectedInheritanceSigner by remember { mutableStateOf<SignerModel?>(null) }
    var removeXfp by rememberSaveable { mutableStateOf("") }
    NunchukTheme {
        if (uiState.isLoading) {
            NcLoadingDialog()
        }
        NcScaffold(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize(),
            snackState = snackState,
            topBar = {
                NcTopAppBar(title = "", actions = {
                    if (uiState.replaceSigners.isNotEmpty()) {
                        IconButton(onClick = { showSheetOptions = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                })
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onCreateWalletClicked,
                    enabled = isEnableCreateWallet
                ) {
                    Text(text = stringResource(R.string.nc_continue_to_create_a_new_wallet))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = stringResource(R.string.nc_which_key_would_you_like_to_replace),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_replace_one_or_multiple_keys),
                    style = NunchukTheme.typography.body
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.walletSigners) { item ->
                        val isInheritanceSlot = uiState.isInheritanceSlot(item.fingerPrint)
                        // An off-chain inheritance key is driven by the sharing method the owner
                        // chose, not by the single verify flag every other key has: it can carry
                        // two artifacts that are verified apart.
                        val claimState = uiState.inheritanceClaimStates[item.fingerPrint]
                            ?.takeIf { isInheritanceSlot }
                        ReplaceKeyCard(
                            modifier = Modifier.padding(top = 16.dp),
                            replacedSigner = uiState.replaceSigners[item.fingerPrint],
                            originalSigner = item,
                            onReplaceClicked = {
                                if (isInheritanceSlot) {
                                    selectedInheritanceSigner = it
                                } else {
                                    onReplaceKeyClicked(it)
                                }
                            },
                            isNeedVerify = uiState.isActiveAssistedWallet &&
                                    !uiState.verifiedSigners.contains(item.fingerPrint) &&
                                    !uiState.verifiedSigners.contains(uiState.replaceSigners[item.fingerPrint]?.fingerPrint) &&
                                    (uiState.replaceSigners[item.fingerPrint]?.type == SignerType.NFC || uiState.replaceSigners[item.fingerPrint]?.tags.orEmpty()
                                        .contains(SignerTag.INHERITANCE)),
                            onVerifyClicked = onVerifyClicked,
                            claimState = claimState,
                            onSetUpClaimOptionsClicked = onSetUpClaimOptionsClicked,
                            onInheritanceBackupClicked = onInheritanceBackupClicked,
                            isMissingBackup = uiState.coldCardBackUpFileName[uiState.replaceSigners[item.fingerPrint]?.fingerPrint].isNullOrEmpty() &&
                                    uiState.replaceSigners[item.fingerPrint]?.type != SignerType.NFC,
                            isReplaced = uiState.replaceSigners.containsKey(item.fingerPrint),
                            onRemoveClicked = {
                                removeXfp = it.fingerPrint
                                showRemoveDialog = true
                            },
                        )
                    }
                }
            }
        }

        if (showConfirmationDialog) {
            NcConfirmationDialog(
                title = stringResource(R.string.nc_confirmation),
                message = stringResource(R.string.nc_confirm_cancel_replacement_desc),
                onPositiveClick = {
                    onCancelReplaceWallet()
                    showConfirmationDialog = false
                },
                onDismiss = {
                    showConfirmationDialog = false
                }
            )
        }

        if (showRemoveDialog) {
            NcConfirmationDialog(
                title = stringResource(R.string.nc_text_warning),
                message = stringResource(R.string.nc_delete_key_msg),
                onPositiveClick = {
                    onRemove(removeXfp)
                    showRemoveDialog = false
                },
                onDismiss = {
                    showRemoveDialog = false
                }
            )
        }

        if (showSheetOptions) {
            NcSelectableBottomSheet(
                options = listOf(
                    stringResource(R.string.nc_cancel_key_replacement),
                ),
                showSelectIndicator = false,
                onSelected = {
                    showConfirmationDialog = true
                    showSheetOptions = false
                },
                onDismiss = {
                    showSheetOptions = false
                }
            )
        } else if (selectedInheritanceSigner != null) {
            NcConfirmationDialog(
                title = stringResource(R.string.nc_text_warning),
                message = stringResource(R.string.nc_inheritance_key_warning),
                onPositiveClick = {
                    onReplaceInheritanceClicked(selectedInheritanceSigner!!)
                    selectedInheritanceSigner = null
                },
                onDismiss = {
                    selectedInheritanceSigner = null
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReplaceKeyCard(
    replacedSigner: SignerModel?,
    originalSigner: SignerModel,
    modifier: Modifier = Modifier,
    isReplaced: Boolean = false,
    isNeedVerify: Boolean = false,
    isMissingBackup: Boolean = false,
    /**
     * Set only for an off-chain inheritance slot that has a replacement on it. It outranks
     * [isNeedVerify]: that flag reads the single verification the server keeps per key, which goes
     * green after one artifact and would hide the second half of a "do both" key.
     */
    claimState: InheritanceClaimState? = null,
    onReplaceClicked: (data: SignerModel) -> Unit = {},
    onVerifyClicked: (data: SignerModel) -> Unit = {},
    onSetUpClaimOptionsClicked: (data: SignerModel) -> Unit = {},
    onInheritanceBackupClicked: (data: SignerModel) -> Unit = {},
    onRemoveClicked: (data: SignerModel) -> Unit = {},
) {
    val item = replacedSigner ?: originalSigner
    val showsClaimStatus = claimState != null && replacedSigner != null
    val needsClaimOptions = showsClaimStatus && claimState.isUnset
    val needsClaimVerification =
        showsClaimStatus && !claimState.isUnset && !claimState.isFullyVerified
    val modifier = if (isReplaced.not()) {
        modifier.border(
            BorderStroke(1.dp, colorResource(id = R.color.nc_stroke_primary)),
            RoundedCornerShape(8.dp)
        )
    } else {
        modifier
    }
    Column {
        Box(
            modifier = modifier.background(
                color = if (isReplaced && !(if (showsClaimStatus) needsClaimVerification else isNeedVerify))
                    MaterialTheme.colorScheme.fillSlimeT2
                else
                    colorResource(id = R.color.nc_background),
                shape = RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NcCircleImage(
                        resId = item.toReadableDrawableResId(),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = NunchukTheme.typography.body
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            NcTag(
                                label = item.toReadableSignerType(context = LocalContext.current),
                                backgroundColor = colorResource(
                                    id = R.color.nc_bg_mid_gray
                                ),
                            )
                            if (item.isShowAcctX()) {
                                NcTag(
                                    label = stringResource(R.string.nc_acct_x, item.index),
                                    backgroundColor = colorResource(
                                        id = R.color.nc_bg_mid_gray
                                    ),
                                )
                            }
                        }
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = item.getXfpOrCardIdLabel(),
                            style = NunchukTheme.typography.bodySmall
                        )
                    }
                    if (isReplaced) {
                        if (needsClaimOptions) {
                            NcOutlineButton(
                                modifier = Modifier.height(36.dp),
                                onClick = { onSetUpClaimOptionsClicked(item) },
                            ) {
                                Text(
                                    text = stringResource(R.string.nc_set_up),
                                    style = NunchukTheme.typography.titleSmall,
                                )
                            }
                        } else if (needsClaimVerification) {
                            NcOutlineButton(
                                modifier = Modifier.height(36.dp),
                                onClick = { onInheritanceBackupClicked(item) },
                            ) {
                                Text(
                                    text = if (claimState.needsEncryptedBackupUpload) {
                                        stringResource(R.string.nc_upload_backup)
                                    } else {
                                        stringResource(R.string.nc_verify_backup)
                                    },
                                    style = NunchukTheme.typography.titleSmall,
                                )
                            }
                        } else if (!showsClaimStatus && isNeedVerify) {
                            NcOutlineButton(
                                modifier = Modifier.height(36.dp),
                                onClick = { onVerifyClicked(item) },
                            ) {
                                Text(
                                    text = if (isMissingBackup.not()) stringResource(R.string.nc_verify_backup) else stringResource(
                                        R.string.nc_upload_backup
                                    ),
                                    style = NunchukTheme.typography.titleSmall,
                                )
                            }
                        } else {
                            NcOutlineButton(
                                modifier = Modifier.height(36.dp),
                                onClick = { onRemoveClicked(originalSigner) },
                            ) {
                                Text(
                                    text = stringResource(R.string.nc_remove),
                                    style = NunchukTheme.typography.titleSmall,
                                )
                            }
                        }
                    } else {
                        NcOutlineButton(
                            modifier = Modifier.height(36.dp),
                            onClick = { onReplaceClicked(item) },
                        ) {
                            Text(
                                text = stringResource(R.string.nc_replace),
                                style = NunchukTheme.typography.titleSmall,
                            )
                        }
                    }
                }
                if (showsClaimStatus) {
                    // Full card width, lined up with the text column (48dp icon + 8dp gap).
                    InheritanceClaimStatusRow(
                        modifier = Modifier.padding(start = 56.dp),
                        claimState = claimState,
                    )
                }
            }
        }

        if (replacedSigner != null) {
            Row(
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_replace),
                    contentDescription = "Replace icon"
                )

                Text(
                    text = "Replacing ${originalSigner.name} (${originalSigner.getXfpOrCardIdLabel()})",
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun ReplaceKeysContentPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    ReplaceKeysContent(
        uiState = ReplaceKeysUiState(
            walletSigners = signers,
            replaceSigners = mapOf(
                signers[0].fingerPrint to signers[1],
            )
        )
    )
}