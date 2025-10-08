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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceKeyType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FindBackupPasswordFragment : MembershipFragment() {
    private val viewModel: FindBackupPasswordViewModel by viewModels()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()
    private val args by navArgs<FindBackupPasswordFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
                val uiState by inheritanceViewModel.state.collectAsStateWithLifecycle()
                FindBackupPasswordContent(
                    remainTime = remainTime,
                    inheritanceKeyType = if (uiState.keyTypes.isNotEmpty()) uiState.keyTypes[args.stepNumber - 1] else InheritanceKeyType.TAPSIGNER,
                    numOfKeys = uiState.keyTypes.size,
                    keyTypes = uiState.keyTypes,
                    stepNumber = args.stepNumber
                ) {
                    if (uiState.keyTypes.size == 2 && args.stepNumber == 1) {
                        findNavController().navigate(
                            FindBackupPasswordFragmentDirections.actionFindBackupPasswordFragmentSelf(
                                2
                            )
                        )
                    } else {
                        findNavController().navigate(FindBackupPasswordFragmentDirections.actionFindBackupPasswordFragmentToInheritanceActivationDateFragment())
                    }
                }
            }
        }
    }
}

@Composable
private fun FindBackupPasswordContent(
    remainTime: Int = 0,
    keyTypes: List<InheritanceKeyType> = emptyList(),
    inheritanceKeyType: InheritanceKeyType = InheritanceKeyType.TAPSIGNER,
    stepNumber: Int = 1,
    numOfKeys: Int = 1,
    onContinueClicked: () -> Unit = {},
) {
    val desc = if (keyTypes.size == 1) {
        if (inheritanceKeyType == InheritanceKeyType.TAPSIGNER) {
            stringResource(id = R.string.nc_find_backup_password_desc)
        } else {
            stringResource(id = R.string.nc_record_your_backup_password_desc)
        }
    } else if (keyTypes.size == 2) {
        val firstType = keyTypes[0]
        val secondType = keyTypes[1]
        if (firstType == InheritanceKeyType.TAPSIGNER && secondType == InheritanceKeyType.TAPSIGNER) {
            if (stepNumber == 1) {
                stringResource(id = R.string.nc_find_backup_password_desc_1)
            } else {
                stringResource(id = R.string.nc_find_backup_password_desc_2, "another")
            }
        } else if (firstType == InheritanceKeyType.COLDCARD && secondType == InheritanceKeyType.COLDCARD) {
            if (stepNumber == 1) {
                stringResource(id = R.string.nc_record_your_backup_password_desc_1)
            } else {
                stringResource(id = R.string.nc_record_your_backup_password_desc_2, "another")
            }
        } else if (inheritanceKeyType == InheritanceKeyType.TAPSIGNER) {
            if (stepNumber == 1) {
                stringResource(id = R.string.nc_find_backup_password_desc_1)
            } else {
                stringResource(id = R.string.nc_find_backup_password_desc_2, "a")
            }
        } else {
            if (stepNumber == 1) {
                stringResource(id = R.string.nc_record_your_backup_password_desc_1)
            } else {
                stringResource(id = R.string.nc_record_your_backup_password_desc_2, "a")
            }
        }
    } else {
        ""
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = if (inheritanceKeyType == InheritanceKeyType.TAPSIGNER) R.drawable.nc_bg_tap_signer_explain else R.drawable.bg_backup_coldcard_illustration,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                if (numOfKeys > 1) {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = "Inheritance Key $stepNumber/$numOfKeys",
                        style = NunchukTheme.typography.title.copy(color = colorResource(id = R.color.nc_beeswax_dark))
                    )
                }
                Text(
                    modifier = Modifier.padding(
                        top = if (numOfKeys > 1) 4.dp else 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    text = if (inheritanceKeyType == InheritanceKeyType.TAPSIGNER) stringResource(id = R.string.nc_find_backup_passwords) else stringResource(
                        R.string.nc_record_your_backup_password
                    ),
                    style = NunchukTheme.typography.heading
                )
                NcSpannedText(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = desc,
                    baseStyle = NunchukTheme.typography.body,
                    styles = mapOf(
                        SpanIndicator('A') to SpanStyle(fontWeight = FontWeight.Bold),
                        SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold),
                    )
                )
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

@PreviewLightDark
@Composable
private fun FindBackupPasswordSingleTapSignerPreview() {
    FindBackupPasswordContent(
        remainTime = 15,
        keyTypes = listOf(InheritanceKeyType.TAPSIGNER),
        inheritanceKeyType = InheritanceKeyType.TAPSIGNER,
        stepNumber = 1,
        numOfKeys = 1
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordSingleColdcardPreview() {
    FindBackupPasswordContent(
        remainTime = 20,
        keyTypes = listOf(InheritanceKeyType.COLDCARD),
        inheritanceKeyType = InheritanceKeyType.COLDCARD,
        stepNumber = 1,
        numOfKeys = 1
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordTwoTapSignersStep1Preview() {
    FindBackupPasswordContent(
        remainTime = 25,
        keyTypes = listOf(InheritanceKeyType.TAPSIGNER, InheritanceKeyType.TAPSIGNER),
        inheritanceKeyType = InheritanceKeyType.TAPSIGNER,
        stepNumber = 1,
        numOfKeys = 2
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordTwoTapSignersStep2Preview() {
    FindBackupPasswordContent(
        remainTime = 30,
        keyTypes = listOf(InheritanceKeyType.TAPSIGNER, InheritanceKeyType.TAPSIGNER),
        inheritanceKeyType = InheritanceKeyType.TAPSIGNER,
        stepNumber = 2,
        numOfKeys = 2
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordTwoColdcardsStep1Preview() {
    FindBackupPasswordContent(
        remainTime = 35,
        keyTypes = listOf(InheritanceKeyType.COLDCARD, InheritanceKeyType.COLDCARD),
        inheritanceKeyType = InheritanceKeyType.COLDCARD,
        stepNumber = 1,
        numOfKeys = 2
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordTwoColdcardsStep2Preview() {
    FindBackupPasswordContent(
        remainTime = 40,
        keyTypes = listOf(InheritanceKeyType.COLDCARD, InheritanceKeyType.COLDCARD),
        inheritanceKeyType = InheritanceKeyType.COLDCARD,
        stepNumber = 2,
        numOfKeys = 2
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordMixedTapSignerFirstStep1Preview() {
    FindBackupPasswordContent(
        remainTime = 45,
        keyTypes = listOf(InheritanceKeyType.TAPSIGNER, InheritanceKeyType.COLDCARD),
        inheritanceKeyType = InheritanceKeyType.TAPSIGNER,
        stepNumber = 1,
        numOfKeys = 2
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordMixedTapSignerFirstStep2Preview() {
    FindBackupPasswordContent(
        remainTime = 50,
        keyTypes = listOf(InheritanceKeyType.TAPSIGNER, InheritanceKeyType.COLDCARD),
        inheritanceKeyType = InheritanceKeyType.COLDCARD,
        stepNumber = 2,
        numOfKeys = 2
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordMixedColdcardFirstStep1Preview() {
    FindBackupPasswordContent(
        remainTime = 55,
        keyTypes = listOf(InheritanceKeyType.COLDCARD, InheritanceKeyType.TAPSIGNER),
        inheritanceKeyType = InheritanceKeyType.COLDCARD,
        stepNumber = 1,
        numOfKeys = 2
    )
}

@PreviewLightDark
@Composable
private fun FindBackupPasswordMixedColdcardFirstStep2Preview() {
    FindBackupPasswordContent(
        remainTime = 60,
        keyTypes = listOf(InheritanceKeyType.COLDCARD, InheritanceKeyType.TAPSIGNER),
        inheritanceKeyType = InheritanceKeyType.TAPSIGNER,
        stepNumber = 2,
        numOfKeys = 2
    )
}