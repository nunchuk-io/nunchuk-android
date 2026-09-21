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

package com.nunchuk.android.signer.components.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.LabelNumberAndDesc
import com.nunchuk.android.compose.NcClickableText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.util.airgapAddKeyTitleRes
import com.nunchuk.android.type.SignerTag
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AirgapIntroFragment : MembershipFragment() {
    private val viewModel: AirgapIntroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val isMembershipFlow = (requireActivity() as AddAirgapSignerActivity).isMembershipFlow
        val signerTag = (requireActivity() as AddAirgapSignerActivity).signerTag
        val replacedXfp = (requireActivity() as AddAirgapSignerActivity).replacedXfp.orEmpty()
        // Some devices export their XPUB from a menu the generic copy does not mention, so the
        // off-chain inheritance guide carries a third step naming it. Scoped to that flow to
        // leave the ordinary add-key and on-chain flows exactly as they are.
        val isOffChainInheritanceSetup = (requireActivity() as AddAirgapSignerActivity)
            .onChainAddSignerParam
            ?.let { it.isAddInheritanceOffChainSigner() && !it.isClaiming } == true
        val exportXpubStep = signerTag.exportXpubStepRes().takeIf { isOffChainInheritanceSetup }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
                AirgapIntroContent(
                    remainTime = remainTime,
                    isMembershipFlow = isMembershipFlow,
                    isReplaceKey = replacedXfp.isNotEmpty(),
                    signerTag = signerTag,
                    exportXpubStep = exportXpubStep,
                    onMoreClicked = ::handleShowMore,
                ) {
                    findNavController().navigate(AirgapIntroFragmentDirections.actionAirgapIntroFragmentToAddAirgapSignerFragment())
                }
            }
        }
    }
}

@Composable
private fun AirgapIntroContent(
    remainTime: Int = 0,
    isMembershipFlow: Boolean = true,
    isReplaceKey: Boolean = false,
    signerTag: SignerTag? = null,
    exportXpubStep: Pair<Int, Int>? = null,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    val addKeyToWalletInProgress = isMembershipFlow && !isReplaceKey && remainTime > 0
    val bgResId = when (signerTag) {
        SignerTag.SEEDSIGNER -> R.drawable.bg_airgap_seedsigner_intro
        SignerTag.JADE -> R.drawable.bg_airgap_jade_intro
        SignerTag.PASSPORT -> R.drawable.bg_airgap_passport_intro
        SignerTag.KEYSTONE -> R.drawable.bg_airgap_keystone_intro
        SignerTag.KRUX -> R.drawable.bg_airgap_krux_intro
        else -> R.drawable.bg_airgap_other_intro
    }

    val title = stringResource(id = signerTag.airgapAddKeyTitleRes())
    val copy = signerTag.airgapIntroCopy()
    val context = LocalContext.current
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = bgResId,
                    title = if (addKeyToWalletInProgress) stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ) else "",
                    actions = {
                        if (addKeyToWalletInProgress) {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = title,
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(copy.note),
                    style = NunchukTheme.typography.body
                )
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 1,
                    title = stringResource(id = copy.initializeTitle),
                    titleStyle = NunchukTheme.typography.title
                ) {
                    val guide = copy.initializeGuideLink
                    if (guide == null) {
                        Text(
                            modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                            text = stringResource(id = copy.initializeDesc),
                            style = NunchukTheme.typography.body
                        )
                    } else {
                        val (labelRes, url) = guide
                        NcClickableText(
                            modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                            messages = listOf(
                                ClickAbleText(content = stringResource(id = copy.initializeDesc)),
                                ClickAbleText(content = stringResource(id = labelRes)) {
                                    context.openExternalLink(url)
                                },
                            ),
                            style = NunchukTheme.typography.body
                        )
                    }
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 2,
                    title = stringResource(id = copy.unlockTitle),
                    titleStyle = NunchukTheme.typography.title
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = copy.unlockDesc),
                        style = NunchukTheme.typography.body
                    )
                }
                if (exportXpubStep != null) {
                    val (titleRes, descRes) = exportXpubStep
                    LabelNumberAndDesc(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        index = 3,
                        title = stringResource(id = titleRes),
                        titleStyle = NunchukTheme.typography.title
                    ) {
                        Text(
                            modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                            text = stringResource(id = descRes),
                            style = NunchukTheme.typography.body
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AirgapIntroScreenPreview() {
    AirgapIntroContent()
}

/** Blockstream's own setup guide, linked from step 1 of the Jade instructions. */
private const val JADE_SETUP_GUIDE_URL =
    "https://help.blockstream.com/blockstream-jade/set-up-transact-recover-your-wallet/set-up-jade"

/** Wording of the first two steps, so a device can name itself instead of saying "the device". */
private data class AirgapIntroCopy(
    @StringRes val note: Int,
    @StringRes val initializeTitle: Int,
    @StringRes val initializeDesc: Int,
    /** Linked phrase closing [initializeDesc]: its label and where it points. */
    val initializeGuideLink: Pair<Int, String>? = null,
    @StringRes val unlockTitle: Int,
    @StringRes val unlockDesc: Int,
)

private val genericAirgapIntroCopy = AirgapIntroCopy(
    note = R.string.nc_signer_before_start_note,
    initializeTitle = R.string.nc_signer_before_start_initialize,
    initializeDesc = R.string.nc_signer_before_start_initialize_content,
    unlockTitle = R.string.nc_signer_before_start_device_unlock,
    unlockDesc = R.string.nc_signer_before_start_device_unlock_content,
)

/**
 * Jade reaches this screen only from the off-chain inheritance flow — every other Jade add-key run
 * starts at airgapActionIntroFragment instead (see AddAirgapSignerActivity) — so naming the device
 * here cannot leak into the ordinary flow. Every other device keeps the generic wording.
 */
private fun SignerTag?.airgapIntroCopy(): AirgapIntroCopy = when (this) {
    SignerTag.JADE -> AirgapIntroCopy(
        note = R.string.nc_jade_before_start_note,
        initializeTitle = R.string.nc_jade_before_start_initialize,
        initializeDesc = R.string.nc_jade_before_start_initialize_content,
        initializeGuideLink = R.string.nc_jade_setup_guide_link to JADE_SETUP_GUIDE_URL,
        unlockTitle = R.string.nc_jade_before_start_device_unlock,
        unlockDesc = R.string.nc_jade_before_start_device_unlock_content,
    )

    else -> genericAirgapIntroCopy
}

/**
 * Title and body of the third step for a device whose XPUB export lives somewhere the generic
 * copy does not mention. Null for the devices the two generic steps already cover.
 */
private fun SignerTag?.exportXpubStepRes(): Pair<Int, Int>? = when (this) {
    SignerTag.KRUX -> R.string.nc_krux_export_xpub_title to R.string.nc_krux_export_xpub_desc
    SignerTag.JADE -> R.string.nc_jade_export_xpub_title to R.string.nc_jade_export_xpub_desc
    else -> null
}
