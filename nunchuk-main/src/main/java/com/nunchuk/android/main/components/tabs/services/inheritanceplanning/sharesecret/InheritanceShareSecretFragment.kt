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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceShareSecretFragment : MembershipFragment() {

    private val viewModel: InheritanceShareSecretViewModel by viewModels()
    private val args: InheritanceShareSecretFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceShareSecretScreen(viewModel, args)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceShareSecretEvent.ContinueClick -> {
                    findNavController().navigate(
                        InheritanceShareSecretFragmentDirections.actionInheritanceShareSecretFragmentToInheritanceShareSecretInfoFragment(
                            magicalPhrase = args.magicalPhrase,
                            type = event.type,
                            planFlow = args.planFlow,
                            walletId = args.walletId,
                            sourceFlow = args.sourceFlow
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun InheritanceShareSecretScreen(
    viewModel: InheritanceShareSecretViewModel = viewModel(),
    args: InheritanceShareSecretFragmentArgs
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()

    InheritanceShareSecretContent(
        remainTime = remainTime,
        options = state.options,
        planFlow = args.planFlow,
        onOptionClick = viewModel::onOptionClick,
        onContinueClicked = viewModel::onContinueClick
    )
}

@Composable
private fun InheritanceShareSecretContent(
    remainTime: Int = 0,
    options: List<InheritanceOption> = emptyList(),
    planFlow: Int = InheritancePlanFlow.NONE,
    onOptionClick: (Int) -> Unit = {},
    onContinueClicked: () -> Unit = {}
) = NunchukTheme {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            val title = if (planFlow == InheritancePlanFlow.SETUP) {
                stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                )
            } else {
                ""
            }
            NcTopAppBar(title = title)
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text(
                        modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_share_your_secrets),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        text = stringResource(R.string.nc_share_your_secrets_desc),
                        style = NunchukTheme.typography.body,
                    )
                    options.forEach { item ->
                        OptionItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            isSelected = item.isSelected,
                            desc = stringResource(id = item.desc),
                            title = stringResource(id = item.title)
                        ) {
                            onOptionClick(item.type)
                        }
                    }
                }
            }
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = options.any { it.isSelected },
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    }
}

@Composable
private fun OptionItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier, onClick = onClick,
        border = BorderStroke(
            width = 2.dp, color = if(isSelected) colorResource(id = R.color.nc_text_primary) else MaterialTheme.colorScheme.border
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NcRadioButton(selected = isSelected, onClick = onClick)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = title, style = NunchukTheme.typography.title)
                Text(text = desc, style = NunchukTheme.typography.body)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceShareSecretScreenPreview() {
    InheritanceShareSecretContent(options = initOptions())
}