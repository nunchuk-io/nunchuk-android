package com.nunchuk.android.main.membership.replacekey

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
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
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
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
        snackState = snackState,
        onReplaceKeyClicked = onReplaceKeyClicked,
        onReplaceInheritanceClicked = onReplaceInheritanceClicked,
        onCreateWalletClicked = viewModel::onCreateWallet,
        onCancelReplaceWallet = viewModel::onCancelReplaceWallet,
        onVerifyClicked = onVerifyClicked
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplaceKeysContent(
    uiState: ReplaceKeysUiState = ReplaceKeysUiState(),
    onReplaceKeyClicked: (SignerModel) -> Unit = {},
    onReplaceInheritanceClicked: (SignerModel) -> Unit = {},
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onCreateWalletClicked: () -> Unit = {},
    onCancelReplaceWallet: () -> Unit = {},
    onVerifyClicked: (SignerModel) -> Unit = {},
) {
    var showSheetOptions by rememberSaveable { mutableStateOf(false) }
    var showConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var selectedInheritanceSigner by remember { mutableStateOf<SignerModel?>(null) }
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
                    enabled = uiState.replaceSigners.isNotEmpty()
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
                        ReplaceKeyCard(
                            modifier = Modifier.padding(top = 16.dp),
                            replacedSigner = uiState.replaceSigners[item.fingerPrint],
                            originalSigner = item,
                            onReplaceClicked = {
                                if (uiState.inheritanceXfps.contains(it.fingerPrint) && uiState.isActiveAssistedWallet) {
                                    selectedInheritanceSigner = it
                                } else {
                                    onReplaceKeyClicked(it)
                                }
                            },
                            isNeedVerify = uiState.isActiveAssistedWallet &&
                                    !uiState.verifiedSigners.contains(item.fingerPrint) &&
                                    !uiState.verifiedSigners.contains(uiState.replaceSigners[item.fingerPrint]?.fingerPrint) &&
                                    (uiState.replaceSigners[item.fingerPrint]?.type == SignerType.NFC || uiState.replaceSigners[item.fingerPrint]?.tags.orEmpty().contains(SignerTag.INHERITANCE)),
                            onVerifyClicked = onVerifyClicked,
                            isReplaced = uiState.replaceSigners.containsKey(item.fingerPrint)
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

@Composable
fun ReplaceKeyCard(
    replacedSigner: SignerModel?,
    originalSigner: SignerModel,
    modifier: Modifier = Modifier,
    isReplaced: Boolean = false,
    isNeedVerify: Boolean = false,
    onReplaceClicked: (data: SignerModel) -> Unit = {},
    onVerifyClicked: (data: SignerModel) -> Unit = {},
) {
    val item = replacedSigner ?: originalSigner
    Column {
        Box(
            modifier = modifier.background(
                color = if (isReplaced && !isNeedVerify)
                    colorResource(id = R.color.nc_green_color)
                else
                    colorResource(id = R.color.nc_beeswax_tint),
                shape = RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                NcCircleImage(
                    resId = item.toReadableDrawableResId(),
                    color = colorResource(id = R.color.nc_white_color)
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
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        NcTag(
                            label = item.toReadableSignerType(context = LocalContext.current),
                            backgroundColor = colorResource(
                                id = R.color.nc_whisper_color
                            ),
                        )
                        if (item.isShowAcctX()) {
                            NcTag(
                                modifier = Modifier.padding(start = 4.dp),
                                label = stringResource(R.string.nc_acct_x, item.index),
                                backgroundColor = colorResource(
                                    id = R.color.nc_whisper_color
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
                    if (isNeedVerify) {
                        NcOutlineButton(
                            modifier = Modifier.height(36.dp),
                            onClick = { onVerifyClicked(item) },
                        ) {
                            Text(text = stringResource(R.string.nc_verify_backup))
                        }
                    }
                } else {
                    NcOutlineButton(
                        modifier = Modifier.height(36.dp),
                        onClick = { onReplaceClicked(item) },
                    ) {
                        Text(text = stringResource(R.string.nc_replace))
                    }
                }
            }
        }

        if (replacedSigner != null) {
            Row(
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(
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
@Preview
private fun ReplaceKeysContentPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    ReplaceKeysContent(
        uiState = ReplaceKeysUiState(walletSigners = signers)
    )
}