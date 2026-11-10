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

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.FallbackInheritanceInfo
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.PrimaryInheritancePolicyInfo
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbackKeyTipRoute
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    /** Configurable on-chain plans only (PLAN-14 / PLAN-05); null keeps the legacy copy. */
    primaryInheritance: PrimaryInheritancePolicyInfo? = null,
    /** The plan’s fallbacks (PLAN-08 / PLAN-11 / PLAN-11b); only read with [primaryInheritance]. */
    fallbacks: List<FallbackInheritanceInfo> = emptyList(),
    onContinueClicked: () -> Unit = {}
) {
    val primary = primaryInheritance?.takeIf { isMiniscriptWallet }
    val primaryFallbacks = if (primary != null) fallbacks else emptyList()
    // The seed phrase is what the Beneficiary will hold, so the copy is about backing it up and the
    // hint about the device it came off. An on-chain timelock key is always handed over that way;
    // off-chain it depends on the sharing method the owner chose.
    val sharesSeedPhrase = seedPhraseKeyIndexes.isNotEmpty()
    val explainsSeedPhrase = isMiniscriptWallet || sharesSeedPhrase
    // The screen speaks about the keys it explains, which off-chain is only the ones the Beneficiary
    // receives as a seed phrase — the others were covered by their Backup Password screen.
    val keysExplained = when {
        primary != null -> primary.sharedSeedPhraseCount + primaryFallbacks.sumOf { it.totalKeys }
        isMiniscriptWallet -> numberOfKey
        else -> seedPhraseKeyIndexes.size.coerceAtLeast(1)
    }
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
                                    when {
                                        primaryFallbacks.isNotEmpty() ->
                                            stringResource(R.string.nc_inheritance_key_tip_primary_hint)

                                        primary?.isCustomized == false ->
                                            stringResource(R.string.nc_inheritance_key_tip_default_primary_hint)

                                        primary != null -> stringResource(R.string.nc_inheritance_key_tip_primary_hint)

                                        else -> pluralStringResource(
                                            R.plurals.nc_inheritance_key_hardware_device_hint,
                                            keysExplained,
                                            keysExplained
                                        )
                                    }
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
                if (primary != null) {
                    PrimaryInheritanceKeysBody(primary, primaryFallbacks)
                } else {
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
}

/**
 * PLAN-14 (guided default) / PLAN-05 (customized): what claims after the timelock, and what to back
 * up. With [fallbacks] (PLAN-08 / PLAN-11 / PLAN-11b) each route gets its own bullet and the backup
 * sentence covers the fallback keys too.
 */
@Composable
private fun PrimaryInheritanceKeysBody(
    primary: PrimaryInheritancePolicyInfo,
    fallbacks: List<FallbackInheritanceInfo> = emptyList(),
) {
    val hasFallbacks = fallbacks.isNotEmpty()
    Text(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = stringResource(
            if (hasFallbacks) R.string.nc_inheritance_and_fallback_keys_used_to_claim
            else R.string.nc_inheritance_keys_used_to_claim
        ),
        style = NunchukTheme.typography.body
    )
    KeyTipBullet(
        when {
            !primary.isCustomized -> stringResource(
                R.string.nc_inheritance_key_tip_default_primary_route,
                primary.requiredSignatures,
                primary.totalKeys,
                primary.ownerKeyCount
            )

            primary.isSingleKey -> stringResource(R.string.nc_inheritance_key_tip_primary_route_single)
            primary.requiredSignatures == primary.totalKeys -> stringResource(
                R.string.nc_inheritance_key_tip_primary_route_all,
                primary.requiredSignatures,
                primary.totalKeys
            )

            else -> stringResource(
                R.string.nc_inheritance_key_tip_primary_route_any,
                primary.requiredSignatures,
                primary.totalKeys
            )
        }
    )
    fallbacks.forEach { fallback -> KeyTipBullet(fallbackKeyTipRoute(fallback)) }
    NcHighlightText(
        modifier = Modifier.padding(16.dp),
        text = stringResource(
            when {
                hasFallbacks -> R.string.nc_inheritance_key_tip_fallback_backup
                primary.isCustomized -> R.string.nc_inheritance_key_tip_primary_backup
                else -> R.string.nc_inheritance_key_tip_default_primary_backup
            }
        ),
        style = NunchukTheme.typography.body
    )
}

@Composable
private fun KeyTipBullet(text: String) {
    Row(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = "•",
            style = NunchukTheme.typography.body
        )
        NcHighlightText(
            modifier = Modifier.weight(1f),
            text = text,
            style = NunchukTheme.typography.body
        )
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

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenDefaultPrimaryPreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        isMiniscriptWallet = true,
        primaryInheritance = PrimaryInheritancePolicyInfo(isCustomized = false, requiredSignatures = 2, totalKeys = 4),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenCustomizedPrimaryPreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        isMiniscriptWallet = true,
        primaryInheritance = PrimaryInheritancePolicyInfo(isCustomized = true, requiredSignatures = 3, totalKeys = 4),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenSingleKeyPrimaryPreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        isMiniscriptWallet = true,
        primaryInheritance = PrimaryInheritancePolicyInfo(isCustomized = true, requiredSignatures = 1, totalKeys = 1),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenDefaultPrimaryWithFallbacksPreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        isMiniscriptWallet = true,
        primaryInheritance = PrimaryInheritancePolicyInfo(isCustomized = false, requiredSignatures = 2, totalKeys = 4),
        fallbacks = listOf(
            FallbackInheritanceInfo(index = 0, requiredSignatures = 1, totalKeys = 1, keys = emptyList(), timelock = null),
            FallbackInheritanceInfo(index = 1, requiredSignatures = 2, totalKeys = 3, keys = emptyList(), timelock = null),
        ),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceKeyTipScreenCustomizedPrimaryWithFallbacksPreview() {
    InheritanceKeyTipContent(
        remainTime = 5,
        isMiniscriptWallet = true,
        primaryInheritance = PrimaryInheritancePolicyInfo(isCustomized = true, requiredSignatures = 3, totalKeys = 4),
        fallbacks = listOf(
            FallbackInheritanceInfo(index = 0, requiredSignatures = 1, totalKeys = 1, keys = emptyList(), timelock = null),
            FallbackInheritanceInfo(index = 1, requiredSignatures = 2, totalKeys = 3, keys = emptyList(), timelock = null),
        ),
    )
}
