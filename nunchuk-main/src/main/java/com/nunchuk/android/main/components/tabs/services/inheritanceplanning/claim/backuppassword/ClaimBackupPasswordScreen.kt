package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.countWords
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claiminput.InheritanceClaimInputEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claiminput.InheritanceClaimInputViewModel
import com.nunchuk.android.model.InheritanceAdditional
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.nunchuk.android.main.R as MainR
import com.nunchuk.android.signer.R as SignerR

@Composable
fun ClaimBackupPasswordScreen(
    magicPhrase: String,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onNoInheritancePlanFound: () -> Unit = {},
    onSuccess: (
        signers: List<SignerModel>,
        magic: String,
        inheritanceAdditional: InheritanceAdditional,
        derivationPaths: List<String>
    ) -> Unit = { _, _, _, _ -> },
    viewModel: InheritanceClaimInputViewModel = hiltViewModel(),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is InheritanceClaimInputEvent.Loading -> {
                    isLoading = event.isLoading
                }
                is InheritanceClaimInputEvent.Error -> {
                    isLoading = false
                    errorMessage = event.message
                }
                is InheritanceClaimInputEvent.NoInheritanceClaimFound -> {
                    isLoading = false
                    onNoInheritancePlanFound()
                }
                is InheritanceClaimInputEvent.GetInheritanceStatusSuccess -> {
                    isLoading = false
                    onSuccess(
                        event.signers,
                        event.magic,
                        event.inheritanceAdditional,
                        event.derivationPaths
                    )
                }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            coroutineScope.launch {
                snackState.showNunchukSnackbar(
                    message = error,
                    type = NcToastType.ERROR
                )
                errorMessage = null
            }
        }
    }

    ClaimBackupPasswordContent(
        modifier = modifier,
        snackState = snackState,
        onBackPressed = onBackPressed,
        backupPasswords = state.backupPasswords,
        isLoading = isLoading,
        onBackupPasswordTextChange = { text, index ->
            viewModel.updateBackupPassword(text, index)
        },
        onContinueClick = {
            keyboardController?.hide()
            viewModel.downloadBackupKey(magicPhrase)
        },
    )
}

@Composable
private fun ClaimBackupPasswordContent(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    backupPasswords: List<String> = listOf("", ""),
    isLoading: Boolean = false,
    onContinueClick: () -> Unit = {},
    onBackupPasswordTextChange: (String, Int) -> Unit = { _, _ -> },
) {
    if (isLoading) {
        NcLoadingDialog()
    }

    NcScaffold(
        modifier = modifier.navigationBarsPadding().imePadding(),
        topBar = {
            NcImageAppBar(
                backgroundRes = MainR.drawable.bg_claim_inheritance_illustration,
                onClosedClicked = onBackPressed,
            )
        },
        snackState = snackState,
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = backupPasswords.any { it.countWords() >= 1 } && !isLoading,
                onClick = onContinueClick,
            ) {
                Text(text = stringResource(id = SignerR.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_claim_inheritance),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    text = "Please enter Backup Password",
                    style = NunchukTheme.typography.body
                )
            }
            item {
                backupPasswords.forEachIndexed { index, backupPassword ->
                    NcTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (index == 0) 24.dp else 16.dp)
                            .padding(horizontal = 16.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        title = if (index == 0) {
                            stringResource(id = MainR.string.nc_backup_download)
                        } else {
                            stringResource(
                                id = MainR.string.nc_backup_download_optional,
                                index + 1
                            )
                        },
                        value = TextFieldValue(
                            text = backupPassword,
                            selection = TextRange(backupPassword.length)
                        ),
                        onValueChange = {
                            onBackupPasswordTextChange(it.text, index)
                        },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ClaimBackupPasswordScreenPreview() {
    NunchukTheme {
        ClaimBackupPasswordContent()
    }
}

