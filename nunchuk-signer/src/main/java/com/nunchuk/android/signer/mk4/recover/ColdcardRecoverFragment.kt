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

package com.nunchuk.android.signer.mk4.recover

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.LabelNumberAndDesc
import com.nunchuk.android.compose.NcClickableText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.COLDCARD_GUIDE_URL
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResult
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ColdcardRecoverFragment : MembershipFragment() {
    private val viewModel: ColdcardRecoverViewModel by viewModels()
    private val args: ColdcardRecoverFragmentArgs by navArgs()

    private val launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.parseColdcardSigner(
                uri = uri,
                groupId = (activity as Mk4Activity).groupId,
                newIndex = (activity as Mk4Activity).newIndex
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ColdcardRecoverScreen(viewModel, ::handleShowMore, args.isMembershipFlow)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        ColdcardRecoverEvent.OnOpenGuide -> requireActivity().openExternalLink(
                            COLDCARD_GUIDE_URL
                        )

                        ColdcardRecoverEvent.OnContinue -> launcher.launch("*/*")
                        ColdcardRecoverEvent.CreateSignerSuccess -> {
                            requireActivity().apply {
                                setResult(RESULT_OK)
                                finish()
                            }
                        }

                        is ColdcardRecoverEvent.LoadingEvent -> showOrHideLoading(event.isLoading)
                        is ColdcardRecoverEvent.ShowError -> showError(event.message)
                        ColdcardRecoverEvent.AddSameKey -> showError(getString(R.string.nc_error_add_same_key))
                        ColdcardRecoverEvent.ParseFileError -> showError(getString(R.string.nc_xpubs_file_invalid))
                        ColdcardRecoverEvent.NewIndexNotMatchException -> {
                            requireActivity().apply {
                                setResult(GlobalResult.RESULT_INDEX_NOT_MATCH)
                                finish()
                            }
                        }

                        ColdcardRecoverEvent.ErrorMk4TestNet -> showError(getString(R.string.nc_error_device_in_testnet_msg))
                    }
                }
        }
    }
}

@Composable
private fun ColdcardRecoverScreen(
    viewModel: ColdcardRecoverViewModel = viewModel(),
    onMoreClicked: () -> Unit = {},
    isMembershipFlow: Boolean,
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    ColdcardRecoverContent(
        remainTime = remainTime,
        onContinueClicked = viewModel::onContinueClicked,
        onOpenGuideClicked = viewModel::onOpenGuideClicked,
        isMembershipFlow = isMembershipFlow,
        onMoreClicked = onMoreClicked,
    )
}

@Composable
private fun ColdcardRecoverContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
    onOpenGuideClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    isMembershipFlow: Boolean = false,
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_add_coldcard_view_nfc_intro,
                title = if (isMembershipFlow) stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ) else "",
                actions = {
                    if (isMembershipFlow) {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                }
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_add_your_coldcard),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_ensure_to_following_step),
                    style = NunchukTheme.typography.body
                )
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 1,
                    title = stringResource(id = R.string.nc_init_coldcard),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    NcClickableText(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        messages = listOf(
                            ClickAbleText(content = stringResource(id = R.string.nc_refer_to)),
                            ClickAbleText(
                                content = stringResource(id = R.string.nc_this_starter_guide),
                                onOpenGuideClicked
                            )
                        ),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 2,
                    title = stringResource(id = R.string.nc_unlock_coldcard),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_unlock_device_desc),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 3,
                    title = stringResource(R.string.nc_export_xpubs_from_coldcard),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(R.string.nc_export_xpub_coldcard_desc),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 4,
                    title = stringResource(R.string.nc_import_xpub_into_app),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(R.string.nc_import_xpub_into_app_desc),
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
private fun ColdcardRecoverScreenPreview() {
    ColdcardRecoverContent(
        isMembershipFlow = true
    )
}