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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.timelockinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceTimelockInfoFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceTimelockInfoContent(
                    onContinueClicked = {
                        findNavController().navigate(
                            InheritanceTimelockInfoFragmentDirections
                                .actionInheritanceTimelockInfoFragmentToInheritanceNoteFragment()
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InheritanceTimelockInfoContent(
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_timelock_illustrations,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        0 // Placeholder for time
                    ),
                )
            },
            bottomBar = {
                Column {
                    NcHintMessage(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        messages = listOf(
                            ClickAbleText(stringResource(R.string.nc_timelock_info_hint))
                        ),
                        type = HighlightMessageType.HINT
                    )
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }
                }

            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_about_the_timelock_title),
                    style = NunchukTheme.typography.heading
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_about_the_timelock_description_1),
                    style = NunchukTheme.typography.body
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_about_the_timelock_description_2),
                    style = NunchukTheme.typography.body
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_about_the_timelock_description_3),
                    style = NunchukTheme.typography.body.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceTimelockInfoScreenPreview() {
    InheritanceTimelockInfoContent()
}
