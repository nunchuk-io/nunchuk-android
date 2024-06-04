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

package com.nunchuk.android.signer.tapsigner.intro

import android.nfc.tech.IsoDep
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.BaseChangeTapSignerNameFragment
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.util.handleTapSignerStatus
import com.nunchuk.android.signer.util.showNfcAlreadyAdded
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddTapSignerIntroFragment : BaseChangeTapSignerNameFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args by navArgs<AddTapSignerIntroFragmentArgs>()
    private val viewModel: AddTapSignerIntroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val walletId = (activity as NfcSetupActivity).walletId
        val replacedXfp = (activity as NfcSetupActivity).replacedXfp
        if (walletId.isNotEmpty() && replacedXfp.isNotEmpty()) {
            viewModel.getReplaceSignerName(walletId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AddTapSignerIntroScreen(viewModel, membershipStepManager, args.isMembershipFlow, ::handleShowMore)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observer()
    }

    override fun onBackUpFileReady(path: String) {
        nameNfcViewModel.getMasterSigner()?.let { signer ->
            nfcViewModel.updateMasterSigner(signer)
            findNavController().navigate(
                AddTapSignerIntroFragmentDirections.actionAddTapSignerIntroFragmentToUploadBackUpTapSignerFragment(
                    path, signer.id
                )
            )
        }
    }

    override val signerName: String
        get() = viewModel.getSignerName()

    override val isMembershipFlow: Boolean
        get() = args.isMembershipFlow

    private fun observer() {
        flowObserver(viewModel.event) {
            when (it) {
                AddTapSignerIntroEvent.ContinueEventAddTapSigner -> {
                    (requireActivity() as NfcActionListener).startNfcFlow(
                        BaseNfcActivity.REQUEST_NFC_STATUS
                    )
                }
                is AddTapSignerIntroEvent.GetTapSignerStatusError -> showError(it.e?.message.orUnknownError())
                is AddTapSignerIntroEvent.GetTapSignerStatusSuccess -> requireActivity().handleTapSignerStatus(
                    it.status,
                    onCreateSigner = {
                        handleCreateSigner(it.status.ident.orEmpty())
                    },
                    onSetupNfc = ::handleSetupTapSigner,
                    onSignerExisted = {
                        if (args.isMembershipFlow) {
                            if (viewModel.isKeyAddedToAssistedWallet(it.status.masterSignerId.orEmpty())) {
                                showError(getString(R.string.nc_error_add_same_key))
                            } else {
                                findNavController().navigate(
                                    AddTapSignerIntroFragmentDirections.actionAddTapSignerIntroFragmentToTapSignerIdFragment(
                                        masterSignerId = it.status.masterSignerId.orEmpty(),
                                        isExisted = true
                                    )
                                )
                            }
                        } else if (viewModel.isInAssistedWallet(it.status.masterSignerId.orEmpty())) {
                            NCInfoDialog(requireActivity()).showDialog(
                                message = getString(R.string.nc_key_has_added_change_key),
                                btnYes = getString(R.string.nc_text_yes),
                                btnInfo = getString(R.string.nc_text_no),
                                onYesClick = {
                                    viewModel.getMasterSigner(it.status.masterSignerId.orEmpty())
                                }
                            ).show()
                        } else {
                            requireActivity().showNfcAlreadyAdded()
                        }
                    }
                )
                is AddTapSignerIntroEvent.Loading -> showOrHideLoading(
                    it.isLoading, message = getString(R.string.nc_keep_holding_nfc)
                )

                is AddTapSignerIntroEvent.GetMasterSignerSuccess -> {
                    navigator.openSignerInfoScreen(
                        activityContext = requireActivity(),
                        isMasterSigner = true,
                        id = it.masterSigner.id,
                        masterFingerprint = it.masterSigner.device.masterFingerprint,
                        name = it.masterSigner.name,
                        type = it.masterSigner.type,
                        justAdded = true
                    )
                    requireActivity().finish()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_STATUS }
                    .collect {
                        viewModel.getTapSignerStatus(IsoDep.get(it.tag))
                        nfcViewModel.clearScanInfo()
                    }
            }
        }
    }

    private fun handleCreateSigner(cardIdent: String) {
        if (isMembershipFlow) {
            (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_ADD_KEY)
        } else {
            findNavController().navigate(AddTapSignerIntroFragmentDirections.actionAddTapSignerIntroFragmentToAddNfcNameFragment(cardIdent))
        }
    }

    private fun handleSetupTapSigner() {
        findNavController().navigate(AddTapSignerIntroFragmentDirections.actionAddTapSignerIntroFragmentToSetupChainCodeFragment())
    }
}

@Composable
private fun AddTapSignerIntroScreen(
    viewModel: AddTapSignerIntroViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    isMembershipFlow: Boolean,
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    AddTapSignerIntroScreenContent(viewModel::onContinueClicked, onMoreClicked, remainTime, isMembershipFlow)
}

@Composable
fun AddTapSignerIntroScreenContent(
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    remainTime: Int = 0,
    isMembershipFlow: Boolean = true,
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_tap_signer_chip,
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
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_add_a_tapsigner),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_add_tap_signer_intro_desc),
                    style = NunchukTheme.typography.body
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

@Preview
@Composable
private fun AddTapSignerIntroScreenPreview() {
    AddTapSignerIntroScreenContent(isMembershipFlow = true)
}
