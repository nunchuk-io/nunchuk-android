package com.nunchuk.android.app.miniscript.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.core.R
import com.nunchuk.android.core.miniscript.MiniscriptUtil
import com.nunchuk.android.core.miniscript.formatMiniscript
import com.nunchuk.android.type.AddressType
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class MiniscriptCustomTemplate(
    val template: String = "",
    val addressType: AddressType
)

fun NavGraphBuilder.miniscriptCustomTemplateDestination(
    fromAddWallet: Boolean = false,
    onNext: (String, AddressType?) -> Unit = { _, _ -> },
    onSaveAndBack: (String) -> Unit = {}
) {
    composable<MiniscriptCustomTemplate> { navBackStackEntry ->
        val data: MiniscriptCustomTemplate = navBackStackEntry.toRoute()
        val viewModel: MiniscriptCustomTemplateViewModel = hiltViewModel()
        val event by viewModel.event.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        var showTaprootWarning by remember { mutableStateOf(false) }
        var pendingTemplate by remember { mutableStateOf("") }

        LaunchedEffect(event) {
            when (event) {
                is MiniscriptCustomTemplateEvent.Success -> {
                    val successEvent = event as MiniscriptCustomTemplateEvent.Success
                    onNext(successEvent.template, successEvent.addressType)
                    viewModel.clearEvent()
                }
                is MiniscriptCustomTemplateEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        NcSnackbarVisuals(
                            message = (event as MiniscriptCustomTemplateEvent.Error).message,
                            type = NcToastType.ERROR
                        )
                    )
                    viewModel.clearEvent()
                }
                is MiniscriptCustomTemplateEvent.ShowTaprootWarning -> {
                    pendingTemplate = (event as MiniscriptCustomTemplateEvent.ShowTaprootWarning).template
                    showTaprootWarning = true
                    viewModel.clearEvent()
                }
                is MiniscriptCustomTemplateEvent.AddressTypeChangedToTaproot -> {
                    viewModel.proceedWithTaproot(pendingTemplate)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            NcSnackbarVisuals(
                                message = "Address type changed to Taproot",
                                type = NcToastType.SUCCESS
                            )
                        )
                    }
                }

                null -> {}
            }
        }

        MiniscriptCustomTemplateScreen(
            template = data.template.formatMiniscript(),
            fromAddWallet = fromAddWallet,
            onContinue = { template ->
                if (fromAddWallet) {
                    onSaveAndBack(MiniscriptUtil.revertFormattedMiniscript(template))
                } else {
                    viewModel.createMiniscriptTemplate(MiniscriptUtil.revertFormattedMiniscript(template), data.addressType)
                }
            },
            onSaveAndBack = onSaveAndBack,
            snackbarHostState = snackbarHostState,
            showTaprootWarning = showTaprootWarning,
            onTaprootWarningDismiss = {
                showTaprootWarning = false
            },
            onTaprootWarningConfirm = {
                showTaprootWarning = false
                viewModel.changeToTaprootAndContinue(pendingTemplate)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniscriptCustomTemplateScreen(
    template: String = "",
    fromAddWallet: Boolean = false,
    onContinue: (String) -> Unit = {},
    onSaveAndBack: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    showTaprootWarning: Boolean = false,
    onTaprootWarningDismiss: () -> Unit = {},
    onTaprootWarningConfirm: () -> Unit = {}
) {
    var miniscriptValue by remember { mutableStateOf(template) }
    var hasBeenEdited by remember { mutableStateOf(false) }
    var initialValue by remember { mutableStateOf(template) }

    // Format the initial template if it's provided and not empty
    LaunchedEffect(template) {
        if (template.isNotBlank() && !hasBeenEdited) {
            miniscriptValue = template.formatMiniscript()
            initialValue = template.formatMiniscript()
        }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_miniscript_enter_miniscript),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            snackbarHost = {
                NcSnackBarHost(snackbarHostState)
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = miniscriptValue.isNotBlank(),
                        onClick = {
                            onContinue(miniscriptValue)
                        }) {
                        Text(text = if (fromAddWallet) stringResource(id = R.string.nc_miniscript_save) else stringResource(id = R.string.nc_miniscript_continue))
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    title = stringResource(id = R.string.nc_miniscript_customize_miniscript),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.nc_miniscript_example),
                            style = NunchukTheme.typography.body.copy(
                                color = colorResource(
                                    id = R.color.nc_boulder_color
                                )
                            )
                        )
                    },
                    value = miniscriptValue,
                    minLines = 4,
                    onValueChange = { newValue ->
                        if (!hasBeenEdited && newValue != initialValue && newValue.isNotBlank()) {
                            // First time entering/pasting content - apply formatting
                            // Check if the input looks like unformatted miniscript (no line breaks)
                            if (!newValue.contains('\n') && newValue.length > 20) {
                                val formatted = newValue.formatMiniscript()
                                miniscriptValue = formatted
                            } else {
                                miniscriptValue = newValue
                            }
                            hasBeenEdited = true
                        } else {
                            // Subsequent edits - no formatting
                            miniscriptValue = newValue
                            if (!hasBeenEdited) {
                                hasBeenEdited = true
                            }
                        }
                    }
                )
            }
        }
        
        if (showTaprootWarning) {
            NcConfirmationDialog(
                title = stringResource(id = R.string.nc_miniscript_warning),
                message = stringResource(id = R.string.nc_miniscript_taproot_warning),
                onPositiveClick = onTaprootWarningConfirm,
                onDismiss = onTaprootWarningDismiss,
                positiveButtonText = stringResource(id = R.string.nc_miniscript_continue),
                negativeButtonText = stringResource(id = R.string.nc_miniscript_no)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewMiniscriptConfigTemplateScreen() {
    MiniscriptCustomTemplateScreen()
}