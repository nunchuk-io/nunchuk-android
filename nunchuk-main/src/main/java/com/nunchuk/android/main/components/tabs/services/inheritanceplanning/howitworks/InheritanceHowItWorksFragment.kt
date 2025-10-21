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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.howitworks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret.InheritanceShareSecretType
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceHowItWorksFragment : MembershipFragment() {

    private val args: InheritanceHowItWorksFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InheritanceHowItWorksScreen(
                    type = args.type,
                    onDoneClick = {
                        findNavController().popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun InheritanceHowItWorksScreen(
    type: InheritanceShareSecretType,
    onDoneClick: () -> Unit = {}
) {
    InheritanceHowItWorksContent(
        type = type,
        onDoneClick = onDoneClick
    )
}

@Composable
private fun InheritanceHowItWorksContent(
    type: InheritanceShareSecretType = InheritanceShareSecretType.DIRECT,
    onDoneClick: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritance_how_it_works,
                    title = "",
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onDoneClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_done))
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                item {

                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_how_it_works),
                        style = NunchukTheme.typography.heading
                    )

                    // Description based on type
                    val descRes = when (type) {
                        InheritanceShareSecretType.DIRECT -> R.string.nc_how_it_works_direct_desc
                        InheritanceShareSecretType.INDIRECT -> R.string.nc_how_it_works_indirect_desc
                        InheritanceShareSecretType.JOINT_CONTROL -> R.string.nc_how_it_works_joint_desc
                    }

                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        text = stringResource(descRes),
                        style = NunchukTheme.typography.body
                    )

                    // Method 1: With Nunchuk service
                    NCLabelWithIndex(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        index = 1,
                        label = stringResource(R.string.nc_with_nunchuk_service),
                        style = NunchukTheme.typography.title
                    )

                    NcHighlightText(
                        modifier = Modifier.padding(start = 50.dp, end = 16.dp, top = 4.dp),
                        text = stringResource(R.string.nc_guided_claim_desc),
                        style = NunchukTheme.typography.body,
                    )

                    // Method 2: Without Nunchuk service
                    NCLabelWithIndex(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        index = 2,
                        label = stringResource(R.string.nc_without_nunchuk_service),
                        style = NunchukTheme.typography.title
                    )

                    val selfServeDescRes = when (type) {
                        InheritanceShareSecretType.DIRECT -> R.string.nc_self_serve_recovery_desc_beneficiary
                        InheritanceShareSecretType.INDIRECT -> R.string.nc_self_serve_recovery_desc_trustee
                        InheritanceShareSecretType.JOINT_CONTROL -> R.string.nc_self_serve_recovery_desc_joint
                    }

                    Text(
                        modifier = Modifier.padding(start = 50.dp, end = 16.dp, top = 4.dp),
                        text = stringResource(selfServeDescRes),
                        style = NunchukTheme.typography.body
                    )

                    val bsmsDescRes = when (type) {
                        InheritanceShareSecretType.DIRECT -> R.string.nc_bsms_replaces_magic_phrase_beneficiary
                        InheritanceShareSecretType.INDIRECT -> R.string.nc_bsms_replaces_magic_phrase_trustee
                        InheritanceShareSecretType.JOINT_CONTROL -> R.string.nc_bsms_replaces_magic_phrase_joint
                    }

                    Text(
                        modifier = Modifier.padding(start = 50.dp, end = 16.dp, top = 8.dp),
                        text = stringResource(bsmsDescRes),
                        style = NunchukTheme.typography.body
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceHowItWorksScreenPreview() {
    InheritanceHowItWorksContent(type = InheritanceShareSecretType.DIRECT)
}

@PreviewLightDark
@Composable
private fun InheritanceHowItWorksScreenIndirectPreview() {
    InheritanceHowItWorksContent(type = InheritanceShareSecretType.INDIRECT)
}

@PreviewLightDark
@Composable
private fun InheritanceHowItWorksScreenJointPreview() {
    InheritanceHowItWorksContent(type = InheritanceShareSecretType.JOINT_CONTROL)
}

