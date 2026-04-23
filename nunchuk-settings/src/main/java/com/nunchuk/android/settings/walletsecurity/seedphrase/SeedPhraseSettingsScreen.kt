package com.nunchuk.android.settings.walletsecurity.seedphrase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcRadioOptionWithInput
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.model.DEFAULT_SEED_PHRASE_DELAY_HOURS
import com.nunchuk.android.model.MIN_SEED_PHRASE_DELAY_HOURS
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.walletsecurity.SeedPhraseSettingsRoute
import kotlinx.coroutines.launch

private const val SEED_PHRASE_TOAST_KEY = "seed_phrase_toast"

fun NavController.navigateToSeedPhraseSettings() {
    navigate(SeedPhraseSettingsRoute)
}

fun NavGraphBuilder.seedPhraseSettingsScreen(
    navController: NavController,
) {
    composable<SeedPhraseSettingsRoute> {
        SeedPhraseSettingsScreen(navController = navController)
    }
}

@Composable
internal fun SeedPhraseSettingsScreen(
    viewModel: SeedPhraseSettingsViewModel = hiltViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val errorMessage = stringResource(R.string.nc_seed_phrase_delay_min_error)

    var selectedOption by rememberSaveable(state.savedHours) {
        mutableStateOf(
            if (state.savedHours <= MIN_SEED_PHRASE_DELAY_HOURS)
                SeedPhraseDelayOption.TWO_HOURS else SeedPhraseDelayOption.CUSTOM
        )
    }
    var customHoursInput by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.savedHours) {
        customHoursInput = if (state.savedHours > MIN_SEED_PHRASE_DELAY_HOURS)
            state.savedHours.toString()
        else
            ""
    }

    val currentOption = selectedOption ?: SeedPhraseDelayOption.TWO_HOURS
    val resolvedHours = when (currentOption) {
        SeedPhraseDelayOption.TWO_HOURS -> MIN_SEED_PHRASE_DELAY_HOURS
        SeedPhraseDelayOption.CUSTOM -> customHoursInput.toIntOrNull() ?: 0
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                SeedPhraseSettingsEvent.SavedImmediate -> {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(
                            SEED_PHRASE_TOAST_KEY,
                            context.getString(R.string.nc_seed_phrase_delay_updated)
                        )
                    navController.popBackStack()
                }

                is SeedPhraseSettingsEvent.SavedWithDelay -> {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(
                            SEED_PHRASE_TOAST_KEY,
                            context.getString(
                                R.string.nc_seed_phrase_delay_updated_with_effect,
                                event.oldHours
                            ),
                        )
                    navController.popBackStack()
                }

                SeedPhraseSettingsEvent.NoChange -> {
                    navController.popBackStack()
                }
            }
        }
    }

    SeedPhraseSettingsContent(
        snackState = snackState,
        selectedOption = currentOption,
        customHoursInput = customHoursInput,
        showError = showError,
        showDecreaseConfirmDialog = state.showDecreaseConfirmDialog,
        savedHours = state.savedHours,
        pendingNewHours = state.pendingNewHours,
        onBackClicked = { navController.popBackStack() },
        onOptionSelected = {
            showError = false
            selectedOption = it
        },
        onCustomHoursChanged = { input ->
            showError = false
            customHoursInput = input
        },
        onContinueClicked = {
            if (resolvedHours < MIN_SEED_PHRASE_DELAY_HOURS) {
                showError = true
                scope.launch {
                    snackState.showNunchukSnackbar(
                        message = errorMessage,
                        type = NcToastType.ERROR,
                    )
                }
            } else {
                viewModel.onContinueClicked(resolvedHours)
            }
        },
        onConfirmDecrease = viewModel::onConfirmDecrease,
        onDismissDecreaseDialog = viewModel::onDismissDecreaseDialog,
    )
}

@Composable
private fun SeedPhraseSettingsContent(
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    selectedOption: SeedPhraseDelayOption = SeedPhraseDelayOption.TWO_HOURS,
    customHoursInput: String = "",
    showError: Boolean = false,
    showDecreaseConfirmDialog: Boolean = false,
    savedHours: Int = DEFAULT_SEED_PHRASE_DELAY_HOURS,
    pendingNewHours: Int? = null,
    onBackClicked: () -> Unit = {},
    onOptionSelected: (SeedPhraseDelayOption) -> Unit = {},
    onCustomHoursChanged: (String) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onConfirmDecrease: () -> Unit = {},
    onDismissDecreaseDialog: () -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            snackState = snackState,
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_seed_phrase_settings),
                    textStyle = NunchukTheme.typography.titleLarge,
                    onBackPress = onBackClicked,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(com.nunchuk.android.core.R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.nc_seed_phrase_delay_description),
                    style = NunchukTheme.typography.body,
                )

                Spacer(modifier = Modifier.height(24.dp))

                NcRadioOption(
                    isSelected = selectedOption == SeedPhraseDelayOption.TWO_HOURS,
                    onClick = { onOptionSelected(SeedPhraseDelayOption.TWO_HOURS) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.nc_seed_phrase_2_hours),
                            style = NunchukTheme.typography.body,
                        )
                        NcTag(label = stringResource(com.nunchuk.android.core.R.string.nc_recommended))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                NcRadioOptionWithInput(
                    isSelected = selectedOption == SeedPhraseDelayOption.CUSTOM,
                    hasError = showError,
                    onClick = { onOptionSelected(SeedPhraseDelayOption.CUSTOM) },
                    label = {
                        Text(
                            text = stringResource(R.string.nc_seed_phrase_delay_custom),
                            style = NunchukTheme.typography.body,
                        )
                    },
                    inputValue = customHoursInput,
                    onInputValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            onCustomHoursChanged(input.take(6))
                        }
                    },
                    inputKeyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    inputRightContent = {
                        Text(
                            modifier = Modifier.padding(end = 12.dp),
                            text = stringResource(R.string.nc_hours),
                            style = NunchukTheme.typography.body.copy(
                                color = MaterialTheme.colorScheme.textSecondary,
                            ),
                        )
                    },
                )
            }
        }

        if (showDecreaseConfirmDialog && pendingNewHours != null) {
            NcConfirmationDialog(
                title = stringResource(R.string.nc_seed_phrase_delay_new_waiting_period),
                message = stringResource(
                    R.string.nc_seed_phrase_delay_decrease_message,
                    savedHours,
                    pendingNewHours,
                ),
                positiveButtonText = stringResource(R.string.nc_confirm),
                negativeButtonText = stringResource(com.nunchuk.android.core.R.string.nc_cancel),
                onPositiveClick = onConfirmDecrease,
                onDismiss = onDismissDecreaseDialog,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SeedPhraseSettingsContentPreview() {
    SeedPhraseSettingsContent()
}

@PreviewLightDark
@Composable
private fun SeedPhraseSettingsContentCustomPreview() {
    SeedPhraseSettingsContent(
        selectedOption = SeedPhraseDelayOption.CUSTOM,
        customHoursInput = "5",
    )
}

@PreviewLightDark
@Composable
private fun SeedPhraseSettingsContentErrorPreview() {
    SeedPhraseSettingsContent(
        selectedOption = SeedPhraseDelayOption.CUSTOM,
        customHoursInput = "1",
        showError = true,
    )
}
