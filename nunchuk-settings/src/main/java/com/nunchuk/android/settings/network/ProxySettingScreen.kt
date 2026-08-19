/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.settings.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.settings.R

@Composable
fun ProxySettingScreen(
    viewModel: ProxySettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.saveSuccessEvent) {
        if (uiState.saveSuccessEvent is StateEvent.Unit) {
            viewModel.onHandleSaveSuccessEvent()
            snackState.showSnackbar(
                NcSnackbarVisuals(
                    type = NcToastType.SUCCESS,
                    message = context.getString(R.string.nc_update_saved),
                )
            )
        }
    }

    ProxySettingContent(
        snackState = snackState,
        uiState = uiState,
        onEnableProxyChanged = viewModel::onEnableProxyChanged,
        onHostChanged = viewModel::onHostChanged,
        onPortChanged = viewModel::onPortChanged,
        onUsernameChanged = viewModel::onUsernameChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        hasChanges = viewModel.hasChanges(),
        onSaveClicked = viewModel::save,
    )
}

@Composable
private fun ProxySettingContent(
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    uiState: ProxySettingUiState = ProxySettingUiState(),
    onEnableProxyChanged: (Boolean) -> Unit = {},
    onHostChanged: (String) -> Unit = {},
    onPortChanged: (String) -> Unit = {},
    onUsernameChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    hasChanges: Boolean = false,
    onSaveClicked: () -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            snackState = snackState,
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_proxy_settings),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = hasChanges,
                    onClick = onSaveClicked,
                ) {
                    Text(text = stringResource(R.string.nc_text_save))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.nc_use_socks5_proxy),
                        style = NunchukTheme.typography.body,
                    )
                    NcSwitch(
                        checked = uiState.enableProxy,
                        onCheckedChange = onEnableProxyChanged,
                    )
                }

                NcHintMessage(messages = listOf(ClickAbleText(content = stringResource(R.string.nc_proxy_settings_desc))))

                NcTextField(
                    title = stringResource(R.string.nc_proxy_host),
                    value = uiState.host,
                    enabled = uiState.enableProxy,
                    singleLine = true,
                    error = uiState.hostError?.let { stringResource(R.string.nc_proxy_host_required) },
                    placeholder = { Text(text = DEFAULT_PROXY_HOST) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    onValueChange = onHostChanged,
                )

                NcTextField(
                    title = stringResource(R.string.nc_proxy_port),
                    value = uiState.port,
                    enabled = uiState.enableProxy,
                    singleLine = true,
                    error = uiState.portError?.let { stringResource(R.string.nc_proxy_port_invalid) },
                    placeholder = { Text(text = DEFAULT_PROXY_PORT) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    onValueChange = onPortChanged,
                )

                NcTextField(
                    title = stringResource(R.string.nc_proxy_username),
                    value = uiState.username,
                    enabled = uiState.enableProxy,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    onValueChange = onUsernameChanged,
                )

                NcTextField(
                    title = stringResource(R.string.nc_proxy_password),
                    value = uiState.password,
                    enabled = uiState.enableProxy,
                    singleLine = true,
                    visualTransformation = if (uiState.password.isEmpty()) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    onValueChange = onPasswordChanged,
                )
            }

            if (uiState.isLoading) {
                NcLoadingDialog()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ProxySettingContentPreview() {
    ProxySettingContent(
        uiState = ProxySettingUiState(
            enableProxy = true,
            host = "127.0.0.1",
            port = "9050",
        ),
    )
}
