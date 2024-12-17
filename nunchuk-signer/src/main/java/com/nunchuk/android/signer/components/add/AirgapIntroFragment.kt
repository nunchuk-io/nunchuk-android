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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.LabelNumberAndDesc
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
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
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
                AirgapIntroContent(
                    remainTime = remainTime,
                    isMembershipFlow = isMembershipFlow,
                    isReplaceKey = replacedXfp.isNotEmpty(),
                    signerTag = signerTag,
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
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    val bgResId = when (signerTag) {
        SignerTag.SEEDSIGNER -> R.drawable.bg_airgap_seedsigner_intro
        SignerTag.JADE -> R.drawable.bg_airgap_jade_intro
        SignerTag.PASSPORT -> R.drawable.bg_airgap_passport_intro
        SignerTag.KEYSTONE -> R.drawable.bg_airgap_keystone_intro
        else -> R.drawable.bg_airgap_other_intro
    }

    val title = when (signerTag) {
        SignerTag.SEEDSIGNER -> stringResource(id = R.string.nc_add_seedsigner)
        SignerTag.JADE -> stringResource(id = R.string.nc_add_jade)
        SignerTag.PASSPORT -> stringResource(id = R.string.nc_add_foundation_passport)
        SignerTag.KEYSTONE -> stringResource(id = R.string.nc_add_keystone)
        else -> stringResource(id = R.string.nc_add_an_airgapped_key)
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = bgResId,
                    title = if (isMembershipFlow && !isReplaceKey) stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ) else "",
                    actions = {
                        if (isMembershipFlow && !isReplaceKey) {
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
                    text = stringResource(R.string.nc_signer_before_start_note),
                    style = NunchukTheme.typography.body
                )
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 1,
                    title = stringResource(id = R.string.nc_signer_before_start_initialize),
                    titleStyle = NunchukTheme.typography.title
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_signer_before_start_initialize_content),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 2,
                    title = stringResource(id = R.string.nc_signer_before_start_device_unlock),
                    titleStyle = NunchukTheme.typography.title
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_signer_before_start_device_unlock_content),
                        style = NunchukTheme.typography.body
                    )
                }
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
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