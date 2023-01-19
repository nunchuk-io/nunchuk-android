/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.backupdownload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackupDownloadFragment : Fragment() {

    private val viewModel: KeyRecoveryBackupDownloadViewModel by viewModels()
    private val args: BackupDownloadFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        viewModel.setKeyName(getString(R.string.nc_recovered_key_name, args.backupKey.keyName))
        return ComposeView(requireContext()).apply {
            setContent {
                BackupDownloadScreen(viewModel, args)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is BackupDownloadEvent.ImportTapsignerSuccess -> {
                    findNavController().navigate(
                        BackupDownloadFragmentDirections.actionBackupDownloadFragmentToKeyRecoverySuccessFragment(
                            it.signer
                        )
                    )
                }
                is BackupDownloadEvent.Loading -> showOrHideLoading(loading = it.isLoading)
                is BackupDownloadEvent.ProcessFailure -> showError(it.message)
            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun BackupDownloadScreen(
    viewModel: KeyRecoveryBackupDownloadViewModel = viewModel(),
    args: BackupDownloadFragmentArgs
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackupDownloadScreenContent(
        tapSignerName = args.signer.name,
        cardId = args.signer.cardId,
        password = state.password,
        error = state.error,
        onContinueClick = {
            viewModel.onContinueClicked()
        },
        onTextChange = {
            viewModel.updatePassword(it)
        })
}

@Composable
fun BackupDownloadScreenContent(
    tapSignerName: String = "",
    cardId: String = "",
    password: String = "",
    error: String = "",
    onContinueClick: () -> Unit = {},
    onTextChange: (value: String) -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(title = "")
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_enter_backup_password),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(
                        id = R.string.nc_enter_backup_password_desc,
                        tapSignerName,
                        "••${cardId.takeLast(5)}"
                        ),
                    style = NunchukTheme.typography.body
                )

                NcTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    title = stringResource(id = R.string.nc_backup_download),
                    value = password,
                    error = error,
                    onValueChange = onTextChange
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(top = 16.dp, end = 16.dp, start = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_enter_backup_password_notice))),
                    type = HighlightMessageType.HINT,
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackupDownloadScreenContentPreview() {
    BackupDownloadScreenContent()
}