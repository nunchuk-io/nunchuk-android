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

package com.nunchuk.android.main.membership.key

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
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
import com.nunchuk.android.core.util.CONTACT_EMAIL
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.sendEmail
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddKeyStepFragment : MembershipFragment() {
    private val viewModel by activityViewModels<AddKeyStepViewModel>()

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
                is AddKeyStepEvent.OpenContactUs -> requireActivity().sendEmail(event.email)
                AddKeyStepEvent.OpenAddKeyList -> handleOpenKeyList()
                AddKeyStepEvent.OpenRecoveryQuestion -> handleOpenRecoveryQuestion()
                AddKeyStepEvent.OpenCreateWallet -> handleOpenCreateWallet()
                AddKeyStepEvent.OnMoreClicked -> handleShowMore()
                AddKeyStepEvent.OpenInheritanceSetup -> handleOpenInheritanceSetup()
                is AddKeyStepEvent.OpenRegisterAirgap -> handleOpenRegisterAirgap(event.walletId)
                is AddKeyStepEvent.OpenRegisterColdCard -> handleOpenRegisterColdcard(event.walletId,)
                AddKeyStepEvent.SetupInheritanceSetupDone -> requireActivity().finish()
            }
        }
    }

    private fun handleOpenRegisterColdcard(walletId: String) {
        findNavController().navigate(
            AddKeyStepFragmentDirections.actionAddKeyStepFragmentToRegisterWalletToColdcardFragment(
                walletId,
                viewModel.getRegisterColdcardIndex(),
                viewModel.getRegisterAirgapIndex(),
            )
        )
    }

    private fun handleOpenRegisterAirgap(walletId: String) {
        findNavController().navigate(
            AddKeyStepFragmentDirections.actionAddKeyStepFragmentToRegisterWalletToAirgapFragment(
                viewModel.getRegisterAirgapIndex(),
                walletId
            )
        )
    }

    private fun handleOpenInheritanceSetup() {
        val walletId = viewModel.activeWalletId()
        if (walletId.isNotEmpty()) {
            nunchukNavigator.openInheritancePlanningScreen(
                walletId = walletId,
                activityContext = requireContext(),
                flowInfo = InheritancePlanFlow.SETUP,
                isOpenFromWizard = true
            )
        }
    }

    private fun handleOpenCreateWallet() {
        findNavController().navigate(AddKeyStepFragmentDirections.actionAddKeyStepFragmentToCreateWalletFragment())
    }

    private fun handleOpenKeyList() {
        findNavController().navigate(AddKeyStepFragmentDirections.actionAddKeyStepFragmentToAddKeyListFragment())
    }

    private fun handleOpenRecoveryQuestion() {
        findNavController().navigate(AddKeyStepFragmentDirections.actionAddKeyListFragmentToRecoveryQuestionFragment())
    }
}

@Composable
fun AddKeyStepScreen(viewModel: AddKeyStepViewModel) {
    val isConfigKeyDone by viewModel.isConfigKeyDone.collectAsStateWithLifecycle()
    val isSetupRecoverKeyDone by viewModel.isSetupRecoverKeyDone.collectAsStateWithLifecycle()
    val isCreateWalletDone by viewModel.isCreateWalletDone.collectAsStateWithLifecycle()
    val isRegisterAirgap by viewModel.isRegisterAirgap.collectAsStateWithLifecycle()
    val isRegisterColdcard by viewModel.isRegisterColdcard.collectAsStateWithLifecycle()
    val isSetupInheritanceDone by viewModel.isSetupInheritanceDone.collectAsStateWithLifecycle()
    val groupRemainTime by viewModel.groupRemainTime.collectAsStateWithLifecycle()

    AddKeyStepContent(
        isConfigKeyDone = isConfigKeyDone,
        isSetupRecoverKeyDone = isSetupRecoverKeyDone,
        isCreateWalletDone = isCreateWalletDone && isRegisterAirgap && isRegisterColdcard,
        isShowMoreOption = isCreateWalletDone.not(),
        isSetupInheritanceDone = isSetupInheritanceDone,
        groupRemainTime = groupRemainTime,
        onMoreClicked = viewModel::onMoreClicked,
        onContinueClicked = viewModel::onContinueClicked,
        openContactUs = viewModel::openContactUs,
        plan = viewModel.plan
    )
}

@Composable
fun AddKeyStepContent(
    isConfigKeyDone: Boolean = false,
    isSetupRecoverKeyDone: Boolean = false,
    isCreateWalletDone: Boolean = false,
    isSetupInheritanceDone: Boolean = false,
    isShowMoreOption: Boolean = false,
    groupRemainTime: IntArray = IntArray(4),
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    openContactUs: (mail: String) -> Unit = {},
    plan: MembershipPlan = MembershipPlan.HONEY_BADGER,
) = NunchukTheme {
    val imageBannerId =
        when {
            isCreateWalletDone -> R.drawable.bg_inheritance
            isSetupRecoverKeyDone -> R.drawable.bg_create_a_wallet
            isConfigKeyDone -> R.drawable.bg_setup_recovery_key
            else -> R.drawable.nc_bg_let_s_add_keys
        }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
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
            StepWithEstTime(
                1,
                stringResource(id = R.string.nc_add_your_keys),
                groupRemainTime[0],
                isConfigKeyDone,
                isConfigKeyDone.not()
            )
            if (isConfigKeyDone.not()) {
                NcHintMessage(
                    modifier = Modifier.padding(top = 16.dp, end = 16.dp, start = 16.dp),
                    messages = listOf(ClickAbleText("This step requires hardware keys to complete. If you have not received your hardware after a while, please contact us at"),
                        ClickAbleText(CONTACT_EMAIL) {
                            openContactUs(CONTACT_EMAIL)
                        })
                )
            }
            StepWithEstTime(
                2,
                stringResource(R.string.nc_setup_security_questions),
                groupRemainTime[1],
                isSetupRecoverKeyDone,
                isConfigKeyDone && isSetupRecoverKeyDone.not()
            )
            StepWithEstTime(
                3,
                stringResource(R.string.nc_create_your_wallet),
                groupRemainTime[2],
                isCreateWalletDone,
                isConfigKeyDone && isSetupRecoverKeyDone && isCreateWalletDone.not()
            )
            if (plan == MembershipPlan.HONEY_BADGER) {
                StepWithEstTime(
                    4,
                    stringResource(R.string.nc_set_up_inheritance_plan),
                    groupRemainTime[3],
                    isSetupInheritanceDone,
                    isCreateWalletDone
                )
            }
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

@Composable
fun StepWithEstTime(
    index: Int,
    label: String,
    estInMinutes: Int,
    isCompleted: Boolean,
    isInProgress: Boolean
) {
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
        text = "${stringResource(R.string.nc_step)} $index",
        style = NunchukTheme.typography.titleSmall
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = NunchukTheme.typography.body)
        if (isCompleted) {
            Text(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = colorResource(id = R.color.nc_whisper_color),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                text = stringResource(R.string.nc_text_completed),
                style = NunchukTheme.typography.caption
            )
        } else {
            val modifier = if (isInProgress) Modifier
                .background(
                    color = colorResource(id = R.color.nc_green_color),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
            else Modifier
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.nc_whisper_color),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
            Text(
                modifier = modifier,
                text = stringResource(R.string.nc_est_time_in_mins, estInMinutes),
                style = NunchukTheme.typography.caption
            )
        }
    }
}

@Preview
@Composable
fun AddKeyStepScreenPreview() {
    AddKeyStepContent(
        isSetupRecoverKeyDone = false,
        isConfigKeyDone = false,
        isCreateWalletDone = true
    )
}