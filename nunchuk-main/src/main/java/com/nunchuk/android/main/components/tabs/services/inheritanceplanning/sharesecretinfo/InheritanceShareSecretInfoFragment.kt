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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretType
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardActivity
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InheritanceShareSecretInfoFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: InheritanceShareSecretInfoFragmentArgs by navArgs()
    private val viewModel: InheritanceShareSecretInfoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceShareSecretInfoScreen(viewModel, args) {
                    if (args.planFlow == InheritancePlanFlow.SETUP) {
                        showDialogInfo(args.sourceFlow)
                    }
                }
            }
        }
    }

    private fun showDialogInfo(sourceFlow: Int) {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_inheritance_share_secret_info_dialog_desc),
            onYesClick = {
                if (sourceFlow == InheritanceSourceFlow.GROUP_DASHBOARD) {
                    ActivityManager.popUntil(GroupDashboardActivity::class.java)
                } else {
                    ActivityManager.popUntilRoot()
                }
                if (sourceFlow != InheritanceSourceFlow.WIZARD) {
                    navigator.openWalletDetailsScreen(requireContext(), args.walletId)
                }
            }
        )
    }
}

@Composable
private fun InheritanceShareSecretInfoScreen(
    viewModel: InheritanceShareSecretInfoViewModel = viewModel(),
    args: InheritanceShareSecretInfoFragmentArgs,
    onActionClick: () -> Unit = {}
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    InheritanceShareSecretInfoContent(
        remainTime = remainTime,
        type = args.type,
        magicalPhrase = args.magicalPhrase,
        planFlow = args.planFlow,
        onActionClick = onActionClick
    )
}


@Composable
private fun InheritanceShareSecretInfoContent(
    remainTime: Int = 0,
    magicalPhrase: String = "",
    type: Int = 0,
    planFlow: Int = InheritancePlanFlow.NONE,
    onActionClick: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        val title = if (planFlow == InheritancePlanFlow.SETUP) {
                            stringResource(
                                id = R.string.nc_estimate_remain_time,
                                remainTime
                            )
                        } else {
                            ""
                        }
                        NcImageAppBar(
                            backgroundRes = R.drawable.nc_bg_tap_signer_explain,
                            title = title,
                        )
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
                                    color = NcColor.greyLight, shape = RoundedCornerShape(12.dp)
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
                                    text = magicalPhrase,
                                    style = NunchukTheme.typography.body,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        NCLabelWithIndex(
                            modifier = Modifier.padding(16.dp),
                            index = 2,
                            label = stringResource(R.string.nc_inheritance_share_secret_info_2),
                        )
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

@Preview
@Composable
private fun InheritanceShareSecretInfoScreenPreview() {
    InheritanceShareSecretInfoContent(

    )
}