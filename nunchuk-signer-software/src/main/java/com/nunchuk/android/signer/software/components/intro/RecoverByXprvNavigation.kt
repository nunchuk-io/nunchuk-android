package com.nunchuk.android.signer.software.components.intro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.model.StateEvent
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import com.nunchuk.android.signer.software.R as SignerSoftwareR

@Serializable
data class RecoverByXprvNavKey(
    val masterSignerId: String = "",
)

fun NavGraphBuilder.recoverByXprv(
    onDoneClicked: () -> Unit = {},
    onContinueClicked: (String) -> Unit
) {
    composable<RecoverByXprvNavKey> {
        RecoverByXprvScreen(
            onDoneClicked = onDoneClicked,
            onContinueClicked = onContinueClicked
        )
    }
}

@Composable
fun RecoverByXprvScreen(
    viewModel: RecoverByXprvViewModel = hiltViewModel(),
    onContinueClicked: (String) -> Unit = {},
    onDoneClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val snackState: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.event) {
        if (state.event is StateEvent.Unit) {
            onContinueClicked(state.xprv)
            viewModel.onEventHandled()
        } else if (state.event is StateEvent.String) {
            snackState.showSnackbar(
                NcSnackbarVisuals(
                    message = (state.event as StateEvent.String).data,
                    type = NcToastType.ERROR
                )
            )
            viewModel.onEventHandled()
        }
    }

    RecoverByXprvContent(
        state = state,
        onContinueClicked = viewModel::validateXprv,
        onXprvChanged = viewModel::onXprvChanged,
        onDoneClicked = onDoneClicked,
        snackState = snackState
    )
}

@Composable
fun RecoverByXprvContent(
    state: RecoverByXprvViewState = RecoverByXprvViewState(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onXprvChanged: (String) -> Unit = {},
    onContinueClicked: (String) -> Unit = {},
    onDoneClicked: () -> Unit = {},
) {
    val context = LocalContext.current
    val isViewMode = state.isViewMode
    val coroutineScope = rememberCoroutineScope()

    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (isViewMode) {
                        stringResource(SignerSoftwareR.string.nc_xprv)
                    } else {
                        stringResource(SignerSoftwareR.string.nc_recover_key_via_xprv)
                    },
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = { if (isViewMode) onDoneClicked() else onContinueClicked(state.xprv) },
                    enabled = state.xprv.isNotBlank()
                ) {
                    Text(
                        text = if (isViewMode) {
                            stringResource(id = R.string.nc_text_done)
                        } else {
                            stringResource(id = R.string.nc_text_continue)
                        }
                    )
                }
            },
            snackState = snackState
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                if (!isViewMode) {
                    Text(
                        text = stringResource(SignerSoftwareR.string.nc_please_enter_the_master_private_key_xprv),
                        style = NunchukTheme.typography.body
                    )
                }

                NcTextField(
                    modifier = Modifier.padding(top = if (isViewMode) 0.dp else 24.dp),
                    title = stringResource(SignerSoftwareR.string.nc_xprv),
                    value = state.xprv,
                    minLines = 5,
                    error = state.error,
                    enabled = !isViewMode,
                    readOnly = isViewMode,
                ) {
                    onXprvChanged(it)
                }

                if (isViewMode) {
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        onClick = {
                            context.copyToClipboard("Nunchuk", state.xprv)
                            coroutineScope.launch {
                                snackState.showSnackbar(
                                    NcSnackbarVisuals(
                                        message = context.getString(SignerSoftwareR.string.nc_xprv_copied_to_clipboard),
                                        type = NcToastType.SUCCESS
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = ""
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.nc_copy) + " " + stringResource(
                                SignerSoftwareR.string.nc_xprv
                            ),
                            style = NunchukTheme.typography.title
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecoverByXprvContentPreview() {
    RecoverByXprvContent()
}