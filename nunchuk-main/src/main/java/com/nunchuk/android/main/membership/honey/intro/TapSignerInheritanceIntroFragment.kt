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

package com.nunchuk.android.main.membership.honey.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TapSignerInheritanceIntroFragment : MembershipFragment() {
    private val viewModel: TapSignerInheritanceIntroViewModel by viewModels()

    @Inject
    lateinit var navigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TapSignerInheritanceIntroScreen(viewModel, ::handleShowMore)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        TapSignerInheritanceIntroEvent.OnContinueClicked -> handleAddTapSigner()
                    }
                }
        }
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            findNavController().popBackStack()
            bundle.parcelable<SignerModel>(TapSignerListBottomSheetFragment.EXTRA_SELECTED_SIGNER_ID)
                ?.let {
                    openCreateBackUpTapSigner(it.id)
                } ?: run {
                openSetupTapSigner()
                clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
            }
        }
    }

    private fun openCreateBackUpTapSigner(masterSignerId: String) {
        if (membershipStepManager.isKeyExisted(masterSignerId).not()) {
            navigator.openCreateBackUpTapSigner(
                activity = requireActivity(),
                fromMembershipFlow = true,
                masterSignerId = masterSignerId,
                groupId = (activity as MembershipActivity).groupId
            )
        } else {
            showSameSignerAdded()
        }
    }

    private fun showSameSignerAdded() {
        showError(getString(R.string.nc_error_add_same_key))
    }

    private fun handleAddTapSigner() {
        runCatching {
            if (viewModel.getTapSigners().isNotEmpty()) {
                findNavController().navigate(
                    TapSignerInheritanceIntroFragmentDirections.actionTapSignerInheritanceIntroFragmentToTapSignerListBottomSheetFragment(
                        signers = viewModel.getTapSigners().toTypedArray(),
                        type = SignerType.NFC
                    )
                )
            } else {
                openSetupTapSigner()
                findNavController().popBackStack()
            }
        }
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            groupId = (activity as MembershipActivity).groupId
        )
    }
}

@Composable
private fun TapSignerInheritanceIntroScreen(
    viewModel: TapSignerInheritanceIntroViewModel = viewModel(),
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    TapSignerInheritanceIntroContent(
        onContinueClicked = viewModel::onContinueClicked,
        onMoreClicked = onMoreClicked,
        remainTime = remainTime
    )
}

@Composable
private fun TapSignerInheritanceIntroContent(
    remainTime: Int = 0,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_inheritance_key,
                title = stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ),
                actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_your_inheritance_key),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_inheritance_intro_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(stringResource(R.string.nc_inheritance_intro_hint)))
                )
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
    }
}

@Preview
@Composable
private fun TapSignerInheritanceIntroScreenPreview() {
    TapSignerInheritanceIntroContent(

    )
}