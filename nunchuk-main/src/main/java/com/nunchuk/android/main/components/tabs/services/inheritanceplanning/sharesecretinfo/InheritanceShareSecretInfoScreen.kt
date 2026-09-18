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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceSetupFlowType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.utils.Utils

@Composable
internal fun InheritanceShareSecretInfoScreen(
    viewModel: InheritanceShareSecretInfoViewModel = viewModel(),
    sharedViewModel: InheritancePlanningViewModel = hiltViewModel(),
    type: Int,
    magicalPhrase: String,
    planFlow: Int,
    onContinue: () -> Unit = {},
    onLearnMoreBackupPasswordClicked: () -> Unit = {},
    onLearnMoreSeedPhraseClicked: () -> Unit = {},
    onSaveBsms: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val sharedUiState by sharedViewModel.state.collectAsStateWithLifecycle()
    InheritanceShareSecretInfoContent(
        isMiniscriptWallet = sharedUiState.isMiniscriptWallet,
        remainTime = remainTime,
        type = type,
        magicalPhrase = magicalPhrase,
        planFlow = planFlow,
        onContinue = onContinue,
        onLearnMoreBackupPasswordClicked = onLearnMoreBackupPasswordClicked,
        onLearnMoreSeedPhraseClicked = onLearnMoreSeedPhraseClicked,
        onSaveBsms = onSaveBsms,
        beneficiaryAllocations = sharedUiState.setupOrReviewParam.beneficiaryAllocations,
        setupFlowType = sharedUiState.setupOrReviewParam.setupFlowType,
        claimOptions = sharedUiState.inheritanceClaimOptions,
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
    onLearnMoreBackupPasswordClicked: () -> Unit = {},
    onLearnMoreSeedPhraseClicked: () -> Unit = {},
    onSaveBsms: () -> Unit = {},
    beneficiaryAllocations: List<InheritanceBeneficiaryAllocation> = emptyList(),
    setupFlowType: InheritanceSetupFlowType = InheritanceSetupFlowType.OLD_FLOW,
    claimOptions: List<ClaimOption> = emptyList(),
) {
    val routes = claimOptions.toInheritanceKeyRoutes()
    if (isMiniscriptWallet) {
        InheritanceOnChainShareSecretInfoContent(
            remainTime = remainTime,
            magicalPhrase = magicalPhrase,
            type = type,
            planFlow = planFlow,
            onContinue = onContinue,
            onSaveBsms = onSaveBsms
        )
    } else if (setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY && beneficiaryAllocations.isNotEmpty()) {
        InheritanceOffChainMultiBeneficiaryContent(
            remainTime = remainTime,
            beneficiaryAllocations = beneficiaryAllocations,
            type = type,
            planFlow = planFlow,
            routes = routes,
            onActionClick = onContinue,
            onLearnMoreBackupPasswordClicked = onLearnMoreBackupPasswordClicked,
            onLearnMoreSeedPhraseClicked = onLearnMoreSeedPhraseClicked,
        )
    } else {
        InheritanceOffChainShareSecretInfoContent(
            remainTime = remainTime,
            magicalPhrase = magicalPhrase,
            type = type,
            planFlow = planFlow,
            routes = routes,
            onActionClick = onContinue,
            onLearnMoreBackupPasswordClicked = onLearnMoreBackupPasswordClicked,
            onLearnMoreSeedPhraseClicked = onLearnMoreSeedPhraseClicked,
        )
    }
}

/** The inline "Learn more" that opens the explanation for this route. */
@Composable
private fun ShareSecretRouteText(
    modifier: Modifier = Modifier,
    route: ClaimOption,
    prefix: String = "",
    onLearnMoreBackupPasswordClicked: () -> Unit,
    onLearnMoreSeedPhraseClicked: () -> Unit,
) {
    val onLearnMoreClicked = when (route) {
        ClaimOption.ENCRYPTED_BACKUP -> onLearnMoreBackupPasswordClicked
        ClaimOption.SEED_PHRASE -> onLearnMoreSeedPhraseClicked
    }
    NcClickableText(
        modifier = modifier,
        messages = listOf(
            ClickAbleText(content = prefix + stringResource(id = route.shareSecretLabelRes)),
            ClickAbleText(content = stringResource(id = R.string.nc_learn_more), onLearnMoreClicked)
        ),
        style = NunchukTheme.typography.body
    )
}

@Composable
private fun ShareSecretWarningMessage(
    modifier: Modifier = Modifier,
    type: Int,
    routes: List<ClaimOption>,
) {
    val warning = shareSecretWarning(type = type, routes = routes)
    val text = warning.partyRes
        ?.let { stringResource(warning.textRes, stringResource(it)) }
        ?: stringResource(warning.textRes)
    NcHintMessage(
        modifier = modifier,
        messages = listOf(ClickAbleText(content = text)),
        type = HighlightMessageType.WARNING,
    )
}

/**
 * "Share your secrets" for an off-chain plan with a single Beneficiary.
 *
 * Always two cards, whatever the key's claim options are: the Magic Phrase, then the inheritance key
 * with a bullet per route. The routes are not secrets of their own — they both unlock the same key —
 * so they stay grouped in one card and the title keeps saying two secrets. Under joint control that
 * grouping is what stops the owner from handing one route to each party, which would give both a
 * working copy of the key and leave the Magic Phrase unmatched; the card spells that out.
 */
@Composable
private fun InheritanceOffChainShareSecretInfoContent(
    remainTime: Int = 0,
    magicalPhrase: String = "",
    type: Int = 0,
    planFlow: Int = InheritancePlanFlow.NONE,
    routes: List<ClaimOption> = listOf(ClaimOption.ENCRYPTED_BACKUP),
    onActionClick: () -> Unit = {},
    onLearnMoreBackupPasswordClicked: () -> Unit = {},
    onLearnMoreSeedPhraseClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(
                WindowInsets.statusBars
            ),
            bottomBar = {
                Column {
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
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
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
                        backgroundRes = routes.shareSecretIllustrationRes,
                        title = title,
                    )
                }
                item {
                    NcHighlightText(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(id = shareSecretTitleRes(type)),
                        style = NunchukTheme.typography.body
                    )

                    SecretCard(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        iconRes = R.drawable.ic_security_answer_distribution,
                        title = stringResource(id = R.string.nc_inheritance_secret_magic_phrase_card),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.greyLight,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                text = magicalPhrase.ifEmpty { Utils.maskValue("", isMask = true) },
                                style = NunchukTheme.typography.body,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    SecretCard(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        iconRes = R.drawable.ic_key,
                        title = stringResource(id = R.string.nc_inheritance_secret_key_card),
                    ) {
                        routes.forEach { route ->
                            ShareSecretRouteText(
                                modifier = Modifier.padding(top = 16.dp),
                                route = route,
                                prefix = "•  ",
                                onLearnMoreBackupPasswordClicked = onLearnMoreBackupPasswordClicked,
                                onLearnMoreSeedPhraseClicked = onLearnMoreSeedPhraseClicked,
                            )
                        }
                        if (routes.size > 1 && type == InheritanceShareSecretType.JOINT_CONTROL.ordinal) {
                            Text(
                                modifier = Modifier.padding(top = 16.dp),
                                text = stringResource(id = R.string.nc_inheritance_key_routes_same_party),
                                style = NunchukTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textSecondary,
                            )
                        }
                    }

                    // Reads immediately above the buttons, where it did when it was pinned to
                    // them — it just no longer costs the secrets their room.
                    ShareSecretWarningMessage(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        type = type,
                        routes = routes,
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun InheritanceOffChainMultiBeneficiaryContent(
    remainTime: Int = 0,
    beneficiaryAllocations: List<InheritanceBeneficiaryAllocation> = emptyList(),
    type: Int = 0,
    planFlow: Int = InheritancePlanFlow.NONE,
    routes: List<ClaimOption> = listOf(ClaimOption.ENCRYPTED_BACKUP),
    onActionClick: () -> Unit = {},
    onLearnMoreBackupPasswordClicked: () -> Unit = {},
    onLearnMoreSeedPhraseClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(
                WindowInsets.statusBars
            ),
            bottomBar = {
                Column {
                    ShareSecretWarningMessage(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        type = type,
                        routes = routes,
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
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
                        backgroundRes = routes.shareSecretIllustrationRes,
                        title = title,
                    )
                }
                item {
                    val typeDesc = when (type) {
                        InheritanceShareSecretType.DIRECT.ordinal -> stringResource(id = R.string.nc_multi_beneficiary_share_secret_title_direct)
                        InheritanceShareSecretType.INDIRECT.ordinal -> stringResource(id = R.string.nc_multi_beneficiary_share_secret_title_indirect)
                        InheritanceShareSecretType.JOINT_CONTROL.ordinal -> stringResource(id = R.string.nc_multi_beneficiary_share_secret_title_joint_control)
                        else -> ""
                    }

                    NcHighlightText(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = typeDesc,
                        style = NunchukTheme.typography.body
                    )
                }

                items(beneficiaryAllocations) { allocation ->
                    BeneficiarySecretCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        email = allocation.email,
                        magicalPhrase = allocation.magic,
                        routes = routes,
                        onLearnMoreBackupPasswordClicked = onLearnMoreBackupPasswordClicked,
                        onLearnMoreSeedPhraseClicked = onLearnMoreSeedPhraseClicked,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun BeneficiarySecretCard(
    modifier: Modifier = Modifier,
    email: String,
    magicalPhrase: String,
    routes: List<ClaimOption> = listOf(ClaimOption.ENCRYPTED_BACKUP),
    onLearnMoreBackupPasswordClicked: () -> Unit = {},
    onLearnMoreSeedPhraseClicked: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.strokePrimary,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = email,
            style = NunchukTheme.typography.title,
        )

        NCLabelWithIndex(
            modifier = Modifier.padding(top = 16.dp),
            index = 1,
            label = stringResource(R.string.nc_plan_magical_phrase),
        )

        Box(
            modifier = Modifier
                .padding(start = 34.dp, top = 12.dp)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.greyLight,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                text = magicalPhrase.ifEmpty {
                    Utils.maskValue("", isMask = true)
                },
                style = NunchukTheme.typography.body,
                textAlign = TextAlign.Center
            )
        }

        routes.forEachIndexed { position, route ->
            NCLabelWithIndex(
                modifier = Modifier.padding(top = 16.dp),
                index = position + 2,
            ) {
                ShareSecretRouteText(
                    modifier = Modifier.padding(top = 0.dp),
                    route = route,
                    onLearnMoreBackupPasswordClicked = onLearnMoreBackupPasswordClicked,
                    onLearnMoreSeedPhraseClicked = onLearnMoreSeedPhraseClicked,
                )
            }
        }
    }
}

@Composable
private fun SecretCard(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.strokePrimary,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.greyLight,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                NcIcon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null,
                )
            }
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = title,
                style = NunchukTheme.typography.title,
            )
        }
        content()
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
private fun InheritanceShareSecretSeedPhraseOnlyPreview() {
    InheritanceOffChainShareSecretInfoContent(
        type = InheritanceShareSecretType.DIRECT.ordinal,
        magicalPhrase = "dolphin concert apple",
        routes = listOf(ClaimOption.SEED_PHRASE),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceShareSecretDoBothPreview() {
    InheritanceOffChainShareSecretInfoContent(
        type = InheritanceShareSecretType.DIRECT.ordinal,
        magicalPhrase = "dolphin concert apple",
        routes = listOf(ClaimOption.ENCRYPTED_BACKUP, ClaimOption.SEED_PHRASE),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceShareSecretIndirectSeedPhraseOnlyPreview() {
    InheritanceOffChainShareSecretInfoContent(
        type = InheritanceShareSecretType.INDIRECT.ordinal,
        magicalPhrase = "dolphin concert apple",
        routes = listOf(ClaimOption.SEED_PHRASE),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceJointControlDoBothPreview() {
    InheritanceOffChainShareSecretInfoContent(
        type = InheritanceShareSecretType.JOINT_CONTROL.ordinal,
        magicalPhrase = "dolphin concert apple",
        routes = listOf(ClaimOption.ENCRYPTED_BACKUP, ClaimOption.SEED_PHRASE),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceJointControlSeedPhraseOnlyPreview() {
    InheritanceOffChainShareSecretInfoContent(
        type = InheritanceShareSecretType.JOINT_CONTROL.ordinal,
        magicalPhrase = "dolphin concert apple",
        routes = listOf(ClaimOption.SEED_PHRASE),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceOffChainMultiBeneficiaryDirectPreview() {
    InheritanceOffChainMultiBeneficiaryContent(
        type = InheritanceShareSecretType.DIRECT.ordinal,
        beneficiaryAllocations = previewBeneficiaryAllocations(),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceOffChainMultiBeneficiaryIndirectPreview() {
    InheritanceOffChainMultiBeneficiaryContent(
        type = InheritanceShareSecretType.INDIRECT.ordinal,
        beneficiaryAllocations = previewBeneficiaryAllocations(),
    )
}

@PreviewLightDark
@Composable
private fun InheritanceOffChainMultiBeneficiaryJointPreview() {
    InheritanceOffChainMultiBeneficiaryContent(
        type = InheritanceShareSecretType.JOINT_CONTROL.ordinal,
        beneficiaryAllocations = previewBeneficiaryAllocations(),
    )
}

private fun previewBeneficiaryAllocations() = listOf(
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
)

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
