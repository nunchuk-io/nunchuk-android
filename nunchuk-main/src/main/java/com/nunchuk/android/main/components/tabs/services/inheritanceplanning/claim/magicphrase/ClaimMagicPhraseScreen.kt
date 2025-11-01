package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.nunchuk.android.compose.backgroundPrimary
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.R
import com.nunchuk.android.core.constants.Constants
import com.nunchuk.android.core.util.countWords
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.model.InheritanceClaimingInit
import kotlinx.coroutines.launch
import com.nunchuk.android.main.R as MainR
import com.nunchuk.android.signer.R as SignerR

@Composable
fun ClaimMagicPhraseScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onContinue: (String, InheritanceClaimingInit) -> Unit = { _, _ -> },
    viewModel: ClaimMagicPhraseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (state.isLoading) {
        NcLoadingDialog()
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            coroutineScope.launch {
                snackState.showNunchukSnackbar(
                    message = error,
                    type = NcToastType.ERROR
                )
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(state.initResult) {
        state.initResult?.let { result ->
            onContinue(state.formattedMagicPhrase, result)
            viewModel.clearInitResult()
        }
    }

    state.dialog?.let { dialog ->
        when (dialog) {
            is ClaimMagicPhraseDialog.SubscriptionExpired -> {
                NcInfoDialog(
                    message = stringResource(R.string.nc_expired_inheritance_subscription),
                    positiveButtonText = stringResource(R.string.nc_take_me_reactivate_plan),
                    negativeButtonText = stringResource(R.string.nc_text_do_this_later),
                    onDismiss = { viewModel.clearDialog() },
                    onPositiveClick = {
                        context.openExternalLink(Constants.CLAIM_URL)
                        viewModel.clearDialog()
                    },
                    onNegativeClick = { viewModel.clearDialog() }
                )
            }
            is ClaimMagicPhraseDialog.InActivated -> {
                NcInfoDialog(
                    message = dialog.message,
                    onDismiss = { viewModel.clearDialog() }
                )
            }
            is ClaimMagicPhraseDialog.PleaseComeLater -> {
                NcInfoDialog(
                    title = stringResource(MainR.string.nc_please_come_back_later),
                    message = dialog.message,
                    positiveButtonText = stringResource(R.string.nc_text_got_it),
                    onDismiss = { viewModel.clearDialog() }
                )
            }
            is ClaimMagicPhraseDialog.SecurityDepositRequired -> {
                NcInfoDialog(
                    title = stringResource(MainR.string.nc_security_deposit_required),
                    message = dialog.message,
                    positiveButtonText = stringResource(MainR.string.nc_go_to_website_to_deposit),
                    negativeButtonText = stringResource(R.string.nc_text_got_it),
                    onDismiss = { viewModel.clearDialog() },
                    onPositiveClick = {
                        context.openExternalLink(Constants.CLAIM_URL)
                        viewModel.clearDialog()
                    },
                    onNegativeClick = { viewModel.clearDialog() }
                )
            }
        }
    }

    ClaimMagicPhraseContent(
        modifier = modifier,
        snackState = snackState,
        onBackPressed = onBackPressed,
        magicalPhrase = state.magicalPhrase,
        suggestions = state.suggestions,
        isLoading = state.isLoading,
        onMagicalPhraseTextChange = {
            viewModel.handleInputEvent(it.lowercase())
        },
        onSuggestClick = {
            viewModel.handleSelectWord(it)
        },
        onContinueClick = {
            viewModel.initInheritanceClaiming()
        },
    )
}

@Composable
private fun ClaimMagicPhraseContent(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    magicalPhrase: String = "",
    suggestions: List<String> = emptyList(),
    isLoading: Boolean = false,
    onContinueClick: () -> Unit = {},
    onSuggestClick: (String) -> Unit = {},
    onMagicalPhraseTextChange: (String) -> Unit = {},
) {
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
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
                enabled = magicalPhrase.countWords() >= 1 && !isLoading,
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
                    text = stringResource(MainR.string.nc_enter_magic_phrase_to_look_up),
                    style = NunchukTheme.typography.body
                )
                NcTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .padding(horizontal = 16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    title = stringResource(id = R.string.nc_magical_phrase),
                    value = TextFieldValue(
                        text = magicalPhrase,
                        selection = TextRange(magicalPhrase.length)
                    ),
                    onValueChange = {
                        onMagicalPhraseTextChange(it.text)
                    },
                )
            }
            item {
                LazyRow(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions) {
                        Card(
                            modifier = Modifier,
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.backgroundPrimary
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 8.dp
                            ),
                            onClick = {
                                onSuggestClick(it)
                            }
                        ) {
                            Text(
                                text = it,
                                style = NunchukTheme.typography.body,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ClaimMagicPhraseScreenPreview() {
    NunchukTheme {
        ClaimMagicPhraseContent()
    }
}

