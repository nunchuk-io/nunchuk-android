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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.keytip

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.main.R

@Composable
internal fun InheritanceKeyTipContent(
    remainTime: Int = 0,
    numberOfKey: Int = 1,
    isMiniscriptWallet: Boolean = false,
    seedPhraseKeyIndexes: List<Int> = emptyList(),
    onContinueClicked: () -> Unit = {}
) {
    // The seed phrase is what the Beneficiary will hold, so the copy is about backing it up and the
    // hint about the device it came off. An on-chain timelock key is always handed over that way;
    // off-chain it depends on the sharing method the owner chose.
    val sharesSeedPhrase = seedPhraseKeyIndexes.isNotEmpty()
    val explainsSeedPhrase = isMiniscriptWallet || sharesSeedPhrase
    // The screen speaks about the keys it explains, which off-chain is only the ones the Beneficiary
    // receives as a seed phrase — the others were covered by their Backup Password screen.
    val keysExplained = if (isMiniscriptWallet) numberOfKey else seedPhraseKeyIndexes.size.coerceAtLeast(1)
    // With one key out of several, the heading names which one instead of counting.
    val singleKeyOfSeveral = seedPhraseKeyIndexes.singleOrNull()
        ?.takeIf { !isMiniscriptWallet && numberOfKey > 1 }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritance_key_illustration,
                    title = estimateRemainTimeTitle(remainTime),
                )
            },
            bottomBar = {
                Column {
                    if (explainsSeedPhrase) {
                        NcHintMessage(
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            messages = listOf(
                                ClickAbleText(
                                    pluralStringResource(
                                        R.plurals.nc_inheritance_key_hardware_device_hint,
                                        keysExplained,
                                        keysExplained
                                    )
                                )
                            )
                        )
                    }

                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
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
                    text = if (singleKeyOfSeveral != null) {
                        stringResource(
                            R.string.nc_inheritance_key_index,
                            singleKeyOfSeveral,
                            numberOfKey
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.nc_inheritance_key,
                            keysExplained,
                            keysExplained
                        )
                    },
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(16.dp),
                    text = when {
                        isMiniscriptWallet -> pluralStringResource(
                            R.plurals.nc_inheritance_key_tip_desc_miniscript,
                            numberOfKey,
                            numberOfKey
                        )

                        sharesSeedPhrase -> pluralStringResource(
                            R.plurals.nc_inheritance_key_tip_desc_seed_phrase,
                            keysExplained,
                            keysExplained
                        )

                        else -> stringResource(R.string.nc_inheritance_key_tip_desc)
                    },
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenPreview() {
    InheritanceKeyTipContent(remainTime = 5)
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenSeedPhrasePreview() {
    InheritanceKeyTipContent(remainTime = 5, seedPhraseKeyIndexes = listOf(1))
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenOneOfTwoSeedPhrasePreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        numberOfKey = 2,
        seedPhraseKeyIndexes = listOf(1),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenBothSeedPhrasePreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        numberOfKey = 2,
        seedPhraseKeyIndexes = listOf(1, 2),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenMiniscriptPreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        numberOfKey = 2,
        isMiniscriptWallet = true,
    )
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenMiniscriptSinglePreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        numberOfKey = 1,
        isMiniscriptWallet = true,
    )
}
