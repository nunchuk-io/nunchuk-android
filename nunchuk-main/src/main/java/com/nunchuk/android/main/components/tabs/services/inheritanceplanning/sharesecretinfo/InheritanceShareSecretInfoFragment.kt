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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcClickableText
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretType
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardActivity
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceShareSecretInfoFragment : MembershipFragment() {

    private val args: InheritanceShareSecretInfoFragmentArgs by navArgs()
    private val viewModel: InheritanceShareSecretInfoViewModel by viewModels()
    private val sharedViewModel: InheritancePlanningViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceShareSecretInfoScreen(
                    sharedViewModel = sharedViewModel,
                    viewModel = viewModel,
                    args = args,
                    onContinue = {
                        if (sharedViewModel.isMiniscriptWallet()) {
                            findNavController().navigate(
                                InheritanceShareSecretInfoFragmentDirections.actionInheritanceShareSecretInfoFragmentToInheritanceHowItWorksFragment(
                                    type = InheritanceShareSecretType.entries[args.type]
                                )
                            )
                        } else if (args.planFlow == InheritancePlanFlow.SETUP) {
                            showDialogInfo()
                        } else {
                            handleBack()
                        }
                    },
                    onLearnMoreClicked = {
                        findNavController().navigate(
                            InheritanceShareSecretInfoFragmentDirections.actionInheritanceShareSecretInfoFragmentToInheritanceBackUpDownloadFragment()
                        )
                    },
                    onSaveBsms = {
                        (activity as? InheritancePlanningActivity)?.showSaveShareOption()
                    }
                )
            }
        }
    }

    private fun showDialogInfo() {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_inheritance_share_secret_info_dialog_desc),
            onYesClick = {
                handleBack()
            }
        )
    }

    private fun handleBack() {
        when (args.sourceFlow) {
            InheritanceSourceFlow.GROUP_DASHBOARD -> {
                if (requireActivity() is GroupDashboardActivity) {
                    findNavController().popBackStack(R.id.groupDashboardFragment, false)
                } else {
                    ActivityManager.popUntil(GroupDashboardActivity::class.java)
                }
            }

            InheritanceSourceFlow.SERVICE_TAB -> requireActivity().finish()
            else -> {
                ActivityManager.popUntilRoot()
                if (args.planFlow == InheritancePlanFlow.SETUP && args.sourceFlow == InheritanceSourceFlow.WIZARD) {
                    navigator.openWalletDetailsScreen(requireContext(), args.walletId)
                }
            }
        }
    }
}

@Composable
private fun InheritanceShareSecretInfoScreen(
    viewModel: InheritanceShareSecretInfoViewModel = viewModel(),
    sharedViewModel: InheritancePlanningViewModel = hiltViewModel(),
    args: InheritanceShareSecretInfoFragmentArgs,
    onContinue: () -> Unit = {},
    onLearnMoreClicked: () -> Unit = {},
    onSaveBsms: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val sharedUiState by sharedViewModel.state.collectAsStateWithLifecycle()
    InheritanceShareSecretInfoContent(
        isMiniscriptWallet = sharedUiState.isMiniscriptWallet,
        remainTime = remainTime,
        type = args.type,
        magicalPhrase = args.magicalPhrase,
        planFlow = args.planFlow,
        onContinue = onContinue,
        onLearnMoreClicked = onLearnMoreClicked,
        onSaveBsms = onSaveBsms
    )
}


@Composable
private fun InheritanceShareSecretInfoContent(
    isMiniscriptWallet: Boolean = false,
    remainTime: Int = 0,
    magicalPhrase: String = "",
    type: Int = 0,
    planFlow: Int = InheritancePlanFlow.NONE,
    onContinue: () -> Unit = {},
    onLearnMoreClicked: () -> Unit = {},
    onSaveBsms: () -> Unit = {},
) {
    if (isMiniscriptWallet) {
        InheritanceOnChainShareSecretInfoContent(
            remainTime = remainTime,
            magicalPhrase = magicalPhrase,
            type = type,
            planFlow = planFlow,
            onContinue = onContinue,
            onSaveBsms = onSaveBsms
        )
    } else {
        InheritanceOffChainShareSecretInfoContent(
            remainTime = remainTime,
            magicalPhrase = magicalPhrase,
            type = type,
            planFlow = planFlow,
            onActionClick = onContinue,
            onLearnMoreClicked = onLearnMoreClicked
        )
    }
}

@Composable
private fun InheritanceOffChainShareSecretInfoContent(
    remainTime: Int = 0,
    magicalPhrase: String = "",
    type: Int = 0,
    planFlow: Int = InheritancePlanFlow.NONE,
    onActionClick: () -> Unit = {},
    onLearnMoreClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                val title = if (planFlow == InheritancePlanFlow.SETUP) {
                    stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    )
                } else {
                    ""
                }
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_backup_password_share_secret,
                    title = title,
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding),
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        val typeDesc = when (type) {
                            InheritanceShareSecretType.DIRECT.ordinal -> stringResource(id = R.string.nc_inheritance_share_secret_info_title_direct)
                            InheritanceShareSecretType.INDIRECT.ordinal -> stringResource(id = R.string.nc_inheritance_share_secret_info_title_indirect)
                            InheritanceShareSecretType.JOINT_CONTROL.ordinal -> stringResource(id = R.string.nc_inheritance_share_secret_info_title_joint_control)
                            else -> ""
                        }

                        NcHighlightText(
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            text = typeDesc,
                            style = NunchukTheme.typography.body
                        )
                        NCLabelWithIndex(
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            index = 1,
                            label = stringResource(R.string.nc_plan_magical_phrase),
                        )

                        Box(
                            modifier = Modifier
                                .padding(start = 50.dp, top = 16.dp, end = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.greyLight,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = magicalPhrase.ifEmpty {
                                        Utils.maskValue(
                                            "",
                                            isMask = true
                                        )
                                    },
                                    style = NunchukTheme.typography.body,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        NCLabelWithIndex(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            index = 2,
                        ) {
                            NcClickableText(
                                modifier = Modifier.padding(top = 0.dp),
                                messages = listOf(
                                    ClickAbleText(content = stringResource(id = R.string.nc_inheritance_share_secret_info_2)),
                                    ClickAbleText(
                                        content = stringResource(id = R.string.nc_learn_more),
                                        onLearnMoreClicked
                                    )
                                ),
                                style = NunchukTheme.typography.body
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                val warningDesc = when (type) {
                    InheritanceShareSecretType.DIRECT.ordinal -> stringResource(id = R.string.nc_beneficiary)
                    InheritanceShareSecretType.INDIRECT.ordinal -> stringResource(id = R.string.nc_trustee)
                    InheritanceShareSecretType.JOINT_CONTROL.ordinal -> stringResource(id = R.string.nc_beneficiary_trustee)
                    else -> ""
                }
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(
                        ClickAbleText(
                            content = stringResource(
                                R.string.nc_inheritance_share_secret_info_warning,
                                warningDesc
                            )
                        )
                    ),
                    type = HighlightMessageType.WARNING,
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onActionClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_done))
                }
                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .height(48.dp),
                    onClick = onActionClick,
                ) {
                    Text(text = stringResource(R.string.nc_text_do_this_later))
                }
            }
        }
    }
}

@Composable
private fun InheritanceOnChainShareSecretInfoContent(
    remainTime: Int = 0,
    magicalPhrase: String = "",
    type: Int = 0,
    planFlow: Int = InheritancePlanFlow.NONE,
    onContinue: () -> Unit = {},
    onSaveBsms: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                val title = if (planFlow == InheritancePlanFlow.SETUP) {
                    stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    )
                } else {
                    ""
                }
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_backup_password_share_secret,
                    title = title,
                )
            }, bottomBar = {
                Column {
                    // Warning message
                    val warningDescRes = when (type) {
                        InheritanceShareSecretType.DIRECT.ordinal -> R.string.nc_onchain_warning_beneficiary
                        InheritanceShareSecretType.INDIRECT.ordinal -> R.string.nc_onchain_warning_trustee
                        InheritanceShareSecretType.JOINT_CONTROL.ordinal -> R.string.nc_onchain_warning_joint
                        else -> R.string.nc_onchain_warning_beneficiary
                    }

                    NcHintMessage(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        messages = listOf(
                            ClickAbleText(
                                content = stringResource(warningDescRes)
                            )
                        ),
                        type = HighlightMessageType.WARNING,
                    )

                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinue,
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    // Title based on type
                    val titleRes = when (type) {
                        InheritanceShareSecretType.DIRECT.ordinal -> R.string.nc_onchain_share_secret_title_beneficiary
                        InheritanceShareSecretType.INDIRECT.ordinal -> R.string.nc_onchain_share_secret_title_trustee
                        InheritanceShareSecretType.JOINT_CONTROL.ordinal -> R.string.nc_onchain_share_secret_title_joint
                        else -> R.string.nc_onchain_share_secret_title_beneficiary
                    }

                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(titleRes),
                        style = NunchukTheme.typography.body
                    )

                    // Item 1: Trustee keeps (for JOINT_CONTROL) or Beneficiary keeps (for DIRECT) or Trustee keeps (for INDIRECT)
                    val item1LabelRes = when (type) {
                        InheritanceShareSecretType.DIRECT.ordinal -> R.string.nc_onchain_item1_label_beneficiary
                        InheritanceShareSecretType.INDIRECT.ordinal -> R.string.nc_onchain_item1_label_trustee
                        InheritanceShareSecretType.JOINT_CONTROL.ordinal -> R.string.nc_onchain_item1_label_trustee
                        else -> R.string.nc_onchain_item1_label_beneficiary
                    }

                    NCLabelWithIndex(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        index = 1,
                        label = stringResource(item1LabelRes),
                    )

                    // Magic Phrase box
                    Box(
                        modifier = Modifier
                            .padding(start = 50.dp, top = 16.dp, end = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.greyLight,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = magicalPhrase.ifEmpty {
                                    Utils.maskValue(
                                        "",
                                        isMask = true
                                    )
                                },
                                style = NunchukTheme.typography.body,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 50.dp, top = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                            .clickable(onClick = onSaveBsms)
                            .background(
                                color = MaterialTheme.colorScheme.greyLight,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.nc_fallback_option_bsms_file),
                            style = NunchukTheme.typography.body
                        )

                        NcIcon(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = "Download icon",
                        )
                    }

                    // Item 2: Beneficiary keeps (for JOINT_CONTROL) or same person keeps (for DIRECT/INDIRECT)
                    val item2LabelRes = when (type) {
                        InheritanceShareSecretType.JOINT_CONTROL.ordinal -> R.string.nc_onchain_item2_label_beneficiary
                        else -> R.string.nc_onchain_item2_label
                    }

                    NCLabelWithIndex(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        index = 2,
                        label = stringResource(item2LabelRes),
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceShareSecretInfoScreenPreview() {
    InheritanceShareSecretInfoContent(
        isMiniscriptWallet = false
    )
}

@PreviewLightDark
@Composable
private fun InheritanceOnChainShareSecretInfoDirectPreview() {
    InheritanceOnChainShareSecretInfoContent(
        type = InheritanceShareSecretType.DIRECT.ordinal,
        magicalPhrase = "dolphin concert apple"
    )
}

@PreviewLightDark
@Composable
private fun InheritanceOnChainShareSecretInfoIndirectPreview() {
    InheritanceOnChainShareSecretInfoContent(
        type = InheritanceShareSecretType.INDIRECT.ordinal,
        magicalPhrase = "dolphin concert apple"
    )
}

@PreviewLightDark
@Composable
private fun InheritanceOnChainShareSecretInfoJointPreview() {
    InheritanceOnChainShareSecretInfoContent(
        type = InheritanceShareSecretType.JOINT_CONTROL.ordinal,
        magicalPhrase = "dolphin concert apple"
    )
}