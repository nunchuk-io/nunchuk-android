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

package com.nunchuk.android.main.membership.replacekey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReplaceKeyIntroFragment : Fragment() {
    private val args by navArgs<ReplaceKeyIntroFragmentArgs>()

    private val viewModel: ReplaceKeysViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var isShowIntro by remember { mutableStateOf(false) }

                LaunchedEffect(uiState.isDataLoaded) {
                    if (uiState.isDataLoaded) {
                        if (uiState.replaceSigners.isNotEmpty() || uiState.pendingReplaceXfps.isNotEmpty()) {
                            openKeyReplaceScreen()
                        } else {
                            isShowIntro = true
                        }
                    }
                }

                if (isShowIntro) {
                    ReplaceKeyIntroScreen(
                        onContinueClicked = ::openKeyReplaceScreen,
                    )
                }
            }
        }
    }

    private fun openKeyReplaceScreen() {
        findNavController().navigate(
            ReplaceKeyIntroFragmentDirections.actionReplaceKeyIntroFragmentToReplaceKeysFragment(
                walletId = args.walletId,
                groupId = args.groupId
            )
        )
    }
}

@Composable
fun ReplaceKeyIntroScreen(
    isLoading: Boolean = false,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onContinueClicked: () -> Unit = {}
) = NunchukTheme {
    if (isLoading) {
        NcLoadingDialog()
    }
    NcScaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_roll_over_illustrations,
                backIconRes = R.drawable.ic_close
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(id = R.string.nc_replace_keys),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.nc_replace_key_desc),
                style = NunchukTheme.typography.body
            )
        }
    }
}

@Preview
@Composable
fun ReplaceKeyIntroScreenPreview() {
    ReplaceKeyIntroScreen()
}
