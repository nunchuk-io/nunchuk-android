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

package com.nunchuk.android.signer.tapsigner.backup.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.backup.BackingUpContent
import com.nunchuk.android.signer.components.backup.BackingUpEvent
import com.nunchuk.android.signer.components.backup.BackingUpState
import com.nunchuk.android.signer.components.backup.BackingUpViewModel
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UploadBackUpTapSignerFragment : MembershipFragment() {
    private val args: UploadBackUpTapSignerFragmentArgs by navArgs()
    private val viewModel: BackingUpViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcViewModel.updateMasterSigner(args.masterSignerId)
        val isAddNewKey =
            if (args.isOldKey) false else (requireActivity() as NfcSetupActivity).isAddNewSigner
        val replacedXfp = (requireActivity() as NfcSetupActivity).replacedXfp
        val walletId = (requireActivity() as NfcSetupActivity).walletId
        viewModel.init(
            isAddNewKey = isAddNewKey,
            groupId = (requireActivity() as NfcSetupActivity).groupId,
            signerIndex = (requireActivity() as NfcSetupActivity).signerIndex,
            replacedXfp = replacedXfp,
            walletId = walletId,
            masterSignerId = args.masterSignerId,
            filePath = args.filePath
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                UploadBackUpTapSignerScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.upload()
        flowObserver(viewModel.event) {
            when (it) {
                BackingUpEvent.OnContinueClicked -> {
                    findNavController().navigate(
                        UploadBackUpTapSignerFragmentDirections.actionUploadBackUpTapSignerFragmentToTapSignerBackUpExplainFragment(
                            viewModel.getServerFilePath(),
                            args.masterSignerId
                        ),
                        NavOptions.Builder()
                            .setPopUpTo(findNavController().graph.startDestinationId, true).build()
                    )
                }

                is BackingUpEvent.ShowError -> showError(it.message)
                is BackingUpEvent.KeyVerified -> {
                    NcToastManager.scheduleShowMessage(it.message)
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
private fun UploadBackUpTapSignerScreen(
    viewModel: BackingUpViewModel = viewModel(),
    membershipStepManager: MembershipStepManager
) {
    val state: BackingUpState by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    BackingUpContent(
        onContinueClicked = viewModel::onContinueClicked,
        percentage = state.percent,
        isError = state.isError,
        remainTime = remainTime,
        title = stringResource(R.string.nc_back_up_tapsigner),
        description = stringResource(R.string.nc_back_up_tap_signer_desc)
    )
}
