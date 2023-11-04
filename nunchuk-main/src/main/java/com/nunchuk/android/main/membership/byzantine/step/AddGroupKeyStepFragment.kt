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

package com.nunchuk.android.main.membership.byzantine.step

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.key.StepWithEstTime
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddGroupKeyStepFragment : MembershipFragment() {
    private val viewModel by activityViewModels<AddGroupKeyStepViewModel>()

    override val isCountdown: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AddKeyStepScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                AddKeyStepEvent.OpenAddKeyList -> handleOpenKeyList()
                AddKeyStepEvent.OpenRecoveryQuestion -> handleOpenRecoveryQuestion()
                AddKeyStepEvent.OpenCreateWallet -> handleOpenCreateWallet()
                AddKeyStepEvent.OnMoreClicked -> handleShowMore()
                is AddKeyStepEvent.OpenRegisterAirgap -> handleOpenRegisterAirgap(event.walletId)
                is AddKeyStepEvent.OpenRegisterColdCard -> handleOpenRegisterColdcard(
                    event.walletId,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun handleOpenRegisterColdcard(walletId: String) {
        findNavController().navigate(
            AddGroupKeyStepFragmentDirections.actionAddGroupKeyStepFragmentToRegisterWalletToColdcardFragment(
                walletId,
                viewModel.getRegisterColdcardIndex(),
                viewModel.getRegisterAirgapIndex(),
            )
        )
    }

    private fun handleOpenRegisterAirgap(walletId: String) {
        findNavController().navigate(
            AddGroupKeyStepFragmentDirections.actionAddGroupKeyStepFragmentToRegisterWalletToAirgapFragment(
                walletId,
            )
        )
    }

    private fun handleOpenCreateWallet() {
        findNavController().navigate(AddGroupKeyStepFragmentDirections.actionAddGroupKeyStepFragmentToCreateWalletFragment())
    }

    private fun handleOpenKeyList() {
        val groupId = (activity as MembershipActivity).groupId
        findNavController().navigate(
            AddGroupKeyStepFragmentDirections.actionAddGroupKeyStepFragmentToAddByzantineKeyListFragment(
                groupId
            )
        )
    }

    private fun handleOpenRecoveryQuestion() {
        if (viewModel.isMaster()) {
            findNavController().navigate(AddGroupKeyStepFragmentDirections.actionAddGroupKeyStepFragmentToRecoveryQuestionFragment())
        } else {
            NCInfoDialog(requireActivity())
                .init(message = getString(R.string.nc_security_question_set_by_master_warning))
                .show()
        }
    }
}

@Composable
fun AddKeyStepScreen(viewModel: AddGroupKeyStepViewModel) {
    val isConfigKeyDone by viewModel.isConfigKeyDone.collectAsStateWithLifecycle()
    val isSetupRecoverKeyDone by viewModel.isSetupRecoverKeyDone.collectAsStateWithLifecycle()
    val isCreateWalletDone by viewModel.isCreateWalletDone.collectAsStateWithLifecycle()
    val isRegisterAirgap by viewModel.isRegisterAirgap.collectAsStateWithLifecycle()
    val isRegisterColdcard by viewModel.isRegisterColdcard.collectAsStateWithLifecycle()
    val groupRemainTime by viewModel.groupRemainTime.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AddKeyStepContent(
        uiState = uiState,
        isConfigKeyDone = isConfigKeyDone,
        isSetupRecoverKeyDone = isSetupRecoverKeyDone,
        isCreateWalletDone = isCreateWalletDone && isRegisterAirgap && isRegisterColdcard,
        isShowMoreOption = isCreateWalletDone.not(),
        groupRemainTime = groupRemainTime,
        onMoreClicked = viewModel::onMoreClicked,
        onContinueClicked = viewModel::onContinueClicked,
    )
}

@Composable
fun AddKeyStepContent(
    uiState: AddGroupUiState = AddGroupUiState(),
    isConfigKeyDone: Boolean = false,
    isSetupRecoverKeyDone: Boolean = false,
    isCreateWalletDone: Boolean = false,
    isShowMoreOption: Boolean = false,
    groupRemainTime: IntArray = IntArray(4),
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) = NunchukTheme {
    val imageBannerId =
        when {
            isCreateWalletDone -> R.drawable.bg_inheritance
            isSetupRecoverKeyDone -> R.drawable.bg_create_a_wallet
            isConfigKeyDone -> R.drawable.bg_setup_recovery_key
            else -> R.drawable.nc_bg_let_s_add_keys
        }

    Scaffold(topBar = {
        NcImageAppBar(
            backgroundRes = imageBannerId,
            actions = {
                if (isShowMoreOption) {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            },
            backIconRes = R.drawable.ic_close,
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            StepWithEstTime(
                1,
                stringResource(id = R.string.nc_add_your_keys),
                groupRemainTime[0],
                isConfigKeyDone,
                isConfigKeyDone.not()
            )
            if (isConfigKeyDone.not()) {
                NcHintMessage(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 16.dp, start = 16.dp),
                    messages = listOf(ClickAbleText(stringResource(R.string.nc_this_step_require_hardware_key)))
                )
            }
            if (uiState.isMaster) {
                StepWithEstTime(
                    2,
                    stringResource(R.string.nc_setup_security_questions),
                    groupRemainTime[1],
                    isSetupRecoverKeyDone,
                    isConfigKeyDone && isSetupRecoverKeyDone.not()
                )
            }
            StepWithEstTime(
                if (uiState.isMaster) 3 else 2,
                stringResource(R.string.nc_create_your_wallet),
                groupRemainTime[2],
                isCreateWalletDone,
                isConfigKeyDone && isSetupRecoverKeyDone && isCreateWalletDone.not()
            )
            Spacer(modifier = Modifier.weight(1.0f))
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked
            ) {
                Text(
                    text = if (isConfigKeyDone.not()) stringResource(R.string.nc_start) else stringResource(
                        id = R.string.nc_text_continue
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun AddKeyStepScreenPreview() {
    AddKeyStepContent(
        isConfigKeyDone = false,
        isSetupRecoverKeyDone = false,
        isCreateWalletDone = true,
    )
}