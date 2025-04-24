package com.nunchuk.android.signer.software.components.intro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.signer.software.R

const val recoverByXprvRoute = "recover_by_xprv"

fun NavGraphBuilder.recoverByXprv(
    onContinueClicked: (String) -> Unit
) {
    composable(recoverByXprvRoute) {
        RecoverByXprvScreen(
            onContinueClicked = onContinueClicked
        )
    }
}

@Composable
fun RecoverByXprvScreen(
    viewModel: RecoverByXprvViewModel = hiltViewModel(),
    onContinueClicked: (String) -> Unit = {}
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
        snackState = snackState
    )
}

@Composable
fun RecoverByXprvContent(
    state: RecoverByXprvViewState = RecoverByXprvViewState(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onXprvChanged: (String) -> Unit = {},
    onContinueClicked: (String) -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_recover_key_via_xprv),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = { onContinueClicked(state.xprv) }, enabled = state.xprv.isNotBlank()
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            },
            snackState = snackState
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.nc_please_enter_the_master_private_key_xprv),
                    style = NunchukTheme.typography.body
                )

                NcTextField(
                    modifier = Modifier.padding(top = 24.dp),
                    title = stringResource(R.string.nc_xprv),
                    value = state.xprv,
                    minLines = 5,
                    error = state.error,
                ) {
                    onXprvChanged(it)
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