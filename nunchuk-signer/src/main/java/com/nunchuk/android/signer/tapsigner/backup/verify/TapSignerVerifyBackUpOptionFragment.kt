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

package com.nunchuk.android.signer.tapsigner.backup.verify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.backup.BACKUP_OPTIONS
import com.nunchuk.android.signer.components.backup.BackUpOption
import com.nunchuk.android.signer.components.backup.BackUpOptionType
import com.nunchuk.android.signer.components.backup.VerifyBackUpOptionContent
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TapSignerVerifyBackUpOptionFragment : MembershipFragment() {
    private val args: TapSignerVerifyBackUpOptionFragmentArgs by navArgs()
    private val viewModel: TapSignerVerifyBackUpOptionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TapSignerVerifyBackUpOptionScreen(
                    viewModel = viewModel,
                    membershipStepManager = membershipStepManager,
                    onContinueClicked = {
                        handleVerifyClicked(it)
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvents()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is TapSignerVerifyBackUpOptionEvent.SkipVerificationSuccess -> {
                            requireActivity().finish()
                        }
                        is TapSignerVerifyBackUpOptionEvent.SkipVerificationError -> {
                            // Error already occurred, still close the activity
                            requireActivity().finish()
                        }
                    }
                }
        }
    }

    private fun handleVerifyClicked(option: BackUpOption) {
        when (option.type) {
            BackUpOptionType.BY_APP -> {
                findNavController().navigate(
                    TapSignerVerifyBackUpOptionFragmentDirections.actionTapSignerVerifyBackUpOptionFragmentToCheckBackUpByAppFragment(
                        args.filePath
                    )
                )
            }

            BackUpOptionType.BY_MYSELF -> {
                findNavController().navigate(
                    TapSignerVerifyBackUpOptionFragmentDirections.actionTapSignerVerifyBackUpOptionFragmentToCheckBackUpBySelfFragment(
                        args.filePath,
                        args.masterSignerId
                    )
                )
            }

            BackUpOptionType.SKIP -> {
                NCWarningDialog(requireActivity()).showDialog(
                    title = getString(R.string.nc_confirmation),
                    message = getString(R.string.nc_skip_back_up_desc),
                    onYesClick = {
                        val nfcActivity = requireActivity() as? NfcSetupActivity
                        if (nfcActivity?.isOnChainBackUp == true) {
                            viewModel.skipVerification(
                                groupId = nfcActivity.groupId,
                                masterSignerId = args.masterSignerId
                            )
                        } else {
                            requireActivity().finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TapSignerVerifyBackUpOptionScreen(
    viewModel: TapSignerVerifyBackUpOptionViewModel,
    membershipStepManager: MembershipStepManager,
    onContinueClicked: (BackUpOption) -> Unit = {}
) {
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

    VerifyBackUpOptionContent(
        onContinueClicked = {
            onContinueClicked(it)
        },
        options = BACKUP_OPTIONS,
        remainingTime = remainingTime
    )
}