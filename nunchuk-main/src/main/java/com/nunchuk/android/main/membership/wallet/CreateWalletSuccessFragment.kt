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

package com.nunchuk.android.main.membership.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CreateWalletSuccessFragment : MembershipFragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    @Inject
    lateinit var pushEventManager: PushEventManager

    private val args: CreateWalletSuccessFragmentArgs by navArgs()

    private val viewModel: CreateWalletSuccessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                CreateWalletSuccessScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val groupId = (activity as MembershipActivity).groupId
        if (groupId.isNotEmpty()) {
            viewModel.loadGroup(groupId)
        }
        flowObserver(viewModel.event) {
            when (it) {
                is CreateWalletSuccessEvent.ContinueStepEvent -> {
                    if (args.isReplaceWallet) {

                    } else if (groupId.isEmpty() && it.is2Of4MultisigWallet) {
                        findNavController().navigate(
                            CreateWalletSuccessFragmentDirections.actionCreateWalletSuccessFragmentToAddKeyStepFragment(),
                            NavOptions.Builder()
                                .setPopUpTo(findNavController().graph.startDestinationId, true)
                                .build()
                        )
                    } else {
                        navigator.openWalletDetailsScreen(
                            requireActivity(),
                            args.walletId
                        )
                        ActivityManager.popUntilRoot()
                    }
                }
            }
        }
        lifecycleScope.launch {
            pushEventManager.push(PushEvent.DismissGroupWalletCreatedAlert)
        }
    }
}

@Composable
private fun CreateWalletSuccessScreen(
    viewModel: CreateWalletSuccessViewModel = viewModel(),
) {
    val uiState : CreateWalletSuccessUiState by viewModel.state.collectAsStateWithLifecycle()
    CreateWalletSuccessScreenContent(
        uiState = uiState,
        onContinueClicked = viewModel::onContinueClicked,
    )
}

@Composable
fun CreateWalletSuccessScreenContent(
    uiState : CreateWalletSuccessUiState = CreateWalletSuccessUiState(),
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_wallet_done,
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.isReplaceWallet) {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(com.nunchuk.android.main.R.string.congratulations_a_new_wallet_has_been_created),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.nc_replace_wallet_success_desc),
                        style = NunchukTheme.typography.body
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_create_wallet_success),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.nc_create_wallet_success_desc),
                        style = NunchukTheme.typography.body
                    )

                    if (uiState.isSingleSetup && uiState.allowInheritance) {
                        NcHighlightText(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.nc_create_wallet_success_distribute_setup_desc),
                            style = NunchukTheme.typography.body
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(
                        text = if (uiState.allowInheritance)
                            stringResource(id = R.string.nc_text_done)
                        else stringResource(id = R.string.nc_text_continue)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CreateWalletSuccessScreenPreview() {
    CreateWalletSuccessScreenContent()
}

@Preview
@Composable
private fun ReplaceWalletSuccessScreenPreview() {
    CreateWalletSuccessScreenContent(
        uiState = CreateWalletSuccessUiState(isReplaceWallet = true)
    )
}