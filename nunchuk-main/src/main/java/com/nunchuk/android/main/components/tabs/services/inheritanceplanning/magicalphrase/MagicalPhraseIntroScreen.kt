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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillDenim
import com.nunchuk.android.compose.fillDenim2
import com.nunchuk.android.compose.primaryT1
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceSetupFlowType

@Composable
internal fun MagicalPhraseIntroScreen(
    viewModel: MagicalPhraseIntroViewModel = viewModel(),
    isMiniscriptWallet: Boolean = false
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY) {
        MultiBeneficiaryMagicalPhraseIntroContent(
            remainTime = remainTime,
            beneficiaryAllocations = state.beneficiaryAllocations,
            isResultReady = state.isResultReady,
            onContinueClicked = viewModel::onContinueClicked,
        )
    } else {
        MagicalPhraseIntroContent(
            remainTime = remainTime,
            magicalPhrase = state.magicalPhrase.orEmpty(),
            isMiniscriptWallet = isMiniscriptWallet,
            onContinueClicked = viewModel::onContinueClicked,
        )
    }
}

@Composable
private fun MagicalPhraseIntroContent(
    remainTime: Int = 0,
    magicalPhrase: String = "",
    isMiniscriptWallet: Boolean = false,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    backgroundColor = MaterialTheme.colorScheme.fillDenim,
                    title = estimateRemainTimeTitle(remainTime),
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    }
                )
            },
            bottomBar = {
                Column {
                    if (isMiniscriptWallet) {
                        NcHintMessage(
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            messages = listOf(
                                com.nunchuk.android.core.util.ClickAbleText(
                                    stringResource(R.string.nc_magical_phrase_required_for_claiming)
                                )
                            )
                        )
                    }

                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = magicalPhrase.isNotEmpty(),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
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
                Column(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.fillDenim)
                        .height(215.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
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
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                        }
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_here_is_plan_magical_phrase),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_magical_phrase_desc),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Composable
private fun MultiBeneficiaryMagicalPhraseIntroContent(
    remainTime: Int = 0,
    beneficiaryAllocations: List<InheritanceBeneficiaryAllocation> = emptyList(),
    isResultReady: Boolean = false,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = estimateRemainTimeTitle(remainTime),
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    }
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = isResultReady,
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    Text(
                        modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_beneficiaries_magic_phrases),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_beneficiaries_magic_phrases_desc),
                        style = NunchukTheme.typography.body
                    )
                    NcHighlightText(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_beneficiaries_magic_phrases_backup),
                        style = NunchukTheme.typography.body
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = MaterialTheme.colorScheme.fillDenim2)
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    ) {
                        beneficiaryAllocations.forEachIndexed { index, allocation ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = allocation.email,
                                    style = NunchukTheme.typography.title,
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.White,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        text = allocation.magic,
                                        style = NunchukTheme.typography.body,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.primaryT1
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MagicalPhraseIntroScreenPreview() {
    MagicalPhraseIntroContent(
        remainTime = 5,
        magicalPhrase = "example-magical-phrase-12345",
        isMiniscriptWallet = false,
        onContinueClicked = {}
    )
}

@PreviewLightDark
@Composable
private fun MagicalPhraseIntroScreenMiniscriptPreview() {
    MagicalPhraseIntroContent(
        remainTime = 5,
        magicalPhrase = "example-magical-phrase-12345",
        isMiniscriptWallet = true,
        onContinueClicked = {}
    )
}

@PreviewLightDark
@Composable
private fun MultiBeneficiaryMagicalPhraseIntroPreview() {
    MultiBeneficiaryMagicalPhraseIntroContent(
        remainTime = 5,
        isResultReady = true,
        beneficiaryAllocations = listOf(
            InheritanceBeneficiaryAllocation(
                email = "wife@gmail.com",
                allocationPercent = 50,
                magic = "dolphin concert apple mirror",
            ),
            InheritanceBeneficiaryAllocation(
                email = "son@gmail.com",
                allocationPercent = 25,
                magic = "galaxy piano silver ocean",
            ),
            InheritanceBeneficiaryAllocation(
                email = "daughter@gmail.com",
                allocationPercent = 25,
                magic = "nebula violin pearl forest",
            ),
        ),
    )
}
