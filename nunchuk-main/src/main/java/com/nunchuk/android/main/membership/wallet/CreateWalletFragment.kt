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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.key.AddKeyStepViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateWalletFragment : MembershipFragment() {
    private val viewModel: CreateWalletViewModel by viewModels()
    private val addKeyStepViewModel: AddKeyStepViewModel by activityViewModels()
    private val groupId: String by lazy { (activity as MembershipActivity).groupId }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                CreateWalletScreen(
                    viewModel = viewModel,
                    onMoreClicked = ::handleShowMore,
                    membershipStepManager = membershipStepManager,
                    onContinueClicked = {
                        if (groupId.isNotEmpty()) {
                            findNavController().navigate(
                                CreateWalletFragmentDirections.actionCreateWalletFragmentToPrimaryOwnerFragment(
                                    groupId = groupId,
                                    flow = PrimaryOwnerFlow.SETUP,
                                    walletName = viewModel.state.value.walletName,
                                    walletId = "",
                                ),
                            )
                        } else {
                            viewModel.createQuickWallet()
                        }
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is CreateWalletEvent.Loading -> showOrHideLoading(it.isLoading)
                is CreateWalletEvent.OnCreateWalletSuccess -> {
                    addKeyStepViewModel.requireInheritance(it.walletId)
                    handleCreateWalletSuccess(it)
                }

                is CreateWalletEvent.ShowError -> showError(it.message)
            }
        }
    }

    private fun handleCreateWalletSuccess(event: CreateWalletEvent.OnCreateWalletSuccess) {
        if (event.airgapCount > 0) {
            findNavController().navigate(
                CreateWalletFragmentDirections.actionCreateWalletFragmentToRegisterWalletToAirgapFragment(
                    event.walletId,
                ),
                NavOptions.Builder()
                    .setPopUpTo(findNavController().graph.startDestinationId, true)
                    .build()
            )
        } else {
            findNavController().navigate(
                CreateWalletFragmentDirections.actionCreateWalletFragmentToCreateWalletSuccessFragment(
                    event.walletId
                ),
                NavOptions.Builder()
                    .setPopUpTo(findNavController().graph.startDestinationId, true)
                    .build()
            )
        }
    }
}

@Composable
fun CreateWalletScreen(
    viewModel: CreateWalletViewModel = viewModel(),
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    membershipStepManager: MembershipStepManager,
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateWalletScreenContent(
        onContinueClicked = onContinueClicked,
        onMoreClicked = onMoreClicked,
        onWalletNameTextChange = viewModel::updateWalletName,
        remainTime = remainTime,
        walletName = state.walletName
    )
}

@Composable
fun CreateWalletScreenContent(
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onWalletNameTextChange: (value: String) -> Unit = {},
    remainTime: Int = 0,
    walletName: String = "",
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(stringResource(R.string.nc_estimate_remain_time, remainTime),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_let_create_your_wallet),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(id = R.string.nc_create_your_wallet_desc),
                    style = NunchukTheme.typography.body
                )

                NcTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    title = stringResource(id = R.string.nc_wallet_name),
                    value = walletName,
                    onValueChange = onWalletNameTextChange,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Spacer(modifier = Modifier.weight(1.0f))

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = walletName.isNotEmpty(),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun CreateWalletScreenContentPreview() {
    CreateWalletScreenContent()
}