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

package com.nunchuk.android.main.membership.signer

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.onchaintimelock.checkfirmware.CheckFirmwareActivity
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.CheckFirmwareArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.KeyType
import com.nunchuk.android.signer.SignerIntroEvent
import com.nunchuk.android.signer.SignerIntroScreen
import com.nunchuk.android.signer.SignerIntroViewModel
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.utils.parcelableArrayList
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnChainSignerIntroFragment : MembershipFragment() {
    
    private val args: OnChainSignerIntroFragmentArgs by navArgs()
    
    private val viewModel: SignerIntroViewModel by viewModels()

    private val nfcSetupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val signerModel = result.data?.getParcelableExtra<SignerModel>(GlobalResultKey.EXTRA_SIGNER)
            if (signerModel != null) {
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        GlobalResultKey.EXTRA_SIGNER to signerModel,
                        EXTRA_IS_FROM_NFC_SETUP to true
                    )
                )
                popUpToOnChainTimelockFragment()
            }
        }
    }

    private val checkFirmwareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val shouldOpenNextScreen = result.data?.getBooleanExtra(
                CheckFirmwareActivity.EXTRA_OPEN_NEXT_SCREEN,
                false
            ) ?: false
            
            if (shouldOpenNextScreen) {
                popUpToOnChainTimelockFragment()
            } else {
                val filteredSigners = result.data?.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
                val signerTagName = result.data?.getStringExtra(GlobalResultKey.EXTRA_SIGNER_TAG)
                if (!filteredSigners.isNullOrEmpty()) {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(
                            GlobalResultKey.EXTRA_SIGNERS to ArrayList(filteredSigners),
                            GlobalResultKey.EXTRA_SIGNER_TAG to signerTagName
                        )
                    )
                }
                popUpToOnChainTimelockFragment()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View {
        viewModel.init(args.onChainAddSignerParam)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                // Handle ViewModel events
                LaunchedEffect(Unit) {
                    viewModel.event.collect { event ->
                        when (event) {
                            is SignerIntroEvent.ShowFilteredTapSigners -> {
                                onFilteredTapSignersReady(event.signers)
                            }

                            SignerIntroEvent.OpenSetupTapSigner -> {
                                navigateToSetupTapSigner()
                            }
                        }
                    }
                }

                SignerIntroScreen(
                    keyFlow = args.keyFlow,
                    supportedSigners = args.supportedSigners?.toList() ?: emptyList(),
                    onChainAddSignerParam = args.onChainAddSignerParam,
                    viewModel = viewModel,
                    onClick = { keyType: KeyType ->
                        when (keyType) {
                            KeyType.TAPSIGNER -> handleTapSignerSelection()
                            KeyType.COLDCARD -> handleColdCardSelection()
                            KeyType.JADE -> handleJadeSelection()
                            KeyType.PORTAL -> openPortalScreen()
                            KeyType.SEEDSIGNER -> handleSelectAddAirgapType(SignerTag.SEEDSIGNER)
                            KeyType.KEYSTONE -> handleSelectAddAirgapType(SignerTag.KEYSTONE)
                            KeyType.FOUNDATION -> handleSelectAddAirgapType(SignerTag.PASSPORT)
                            KeyType.LEDGER -> handleHardwareSignerSelection(SignerTag.LEDGER)
                            KeyType.BITBOX -> handleHardwareSignerSelection(SignerTag.BITBOX)
                            KeyType.TREZOR -> handleHardwareSignerSelection(SignerTag.TREZOR)
                            KeyType.SOFTWARE -> openAddSoftwareSignerScreen()
                            KeyType.GENERIC_AIRGAP -> openAddAirSignerIntroScreen()
                        }
                    }
                )
            }
        }
    }

    private fun onFilteredTapSignersReady(filteredSigners: List<SignerModel>) {
        setFragmentResult(
            REQUEST_KEY,
            bundleOf(GlobalResultKey.EXTRA_SIGNERS to ArrayList(filteredSigners))
        )
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun handleTapSignerSelection() {
        if (args.onChainAddSignerParam != null) {
            viewModel.onTapSignerContinueClicked()
        } else {
            navigateToSetupTapSigner()
        }
    }

    private fun handleColdCardSelection() {
        if (args.onChainAddSignerParam != null) {
            navigator.openCheckFirmwareActivity(
                activityContext = requireActivity(),
                launcher = checkFirmwareLauncher,
                args = CheckFirmwareArgs(
                    signerTag = SignerTag.COLDCARD,
                    onChainAddSignerParam = args.onChainAddSignerParam,
                    walletId = args.walletId.orEmpty(),
                    groupId = args.groupId.orEmpty()
                )
            )
        } else {
            openSetupMk4()
            popUpToOnChainTimelockFragment()
        }
    }

    private fun handleJadeSelection() {
        if (args.onChainAddSignerParam != null) {
            navigator.openCheckFirmwareActivity(
                activityContext = requireActivity(),
                launcher = checkFirmwareLauncher,
                args = CheckFirmwareArgs(
                    signerTag = SignerTag.JADE,
                    onChainAddSignerParam = args.onChainAddSignerParam,
                    walletId = args.walletId.orEmpty(),
                    groupId = args.groupId.orEmpty()
                )
            )
        } else {
            handleSelectAddAirgapType(SignerTag.JADE)
        }
    }

    private fun handleHardwareSignerSelection(tag: SignerTag) {
        if (args.onChainAddSignerParam != null) {
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(GlobalResultKey.EXTRA_SIGNER_TAG to tag)
            )
            requireActivity().onBackPressedDispatcher.onBackPressed()
        } else {
            // For non-onChain flows, this should not be called as the buttons are disabled
        }
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            args = AddAirSignerArgs(
                isMembershipFlow = args.onChainAddSignerParam != null,
                tag = tag,
                groupId = args.groupId.orEmpty(),
                walletId = args.walletId.orEmpty(),
                onChainAddSignerParam = args.onChainAddSignerParam,
            )
        )
        popUpToOnChainTimelockFragment()
    }

    private fun openSetupMk4() {
        navigator.openSetupMk4(
            activity = requireActivity(),
            args = SetupMk4Args(
                fromMembershipFlow = args.onChainAddSignerParam != null,
                isFromAddKey = args.onChainAddSignerParam == null,
                groupId = args.groupId.orEmpty(),
                walletId = args.walletId.orEmpty(),
                onChainAddSignerParam = args.onChainAddSignerParam,
            )
        )
    }

    private fun openPortalScreen() {
        navigator.openPortalScreen(
            activity = requireActivity(),
            args = PortalDeviceArgs(
                type = PortalDeviceFlow.SETUP,
                isMembershipFlow = args.walletId?.isNotEmpty() == true || args.onChainAddSignerParam != null,
                walletId = args.walletId.orEmpty(),
                groupId = args.groupId.orEmpty(),
            )
        )
        popUpToOnChainTimelockFragment()
    }

    private fun openAddAirSignerIntroScreen() {
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            args = AddAirSignerArgs(
                isMembershipFlow = args.onChainAddSignerParam != null,
                groupId = args.groupId.orEmpty(),
                walletId = args.walletId.orEmpty(),
                onChainAddSignerParam = args.onChainAddSignerParam,
            )
        )
        popUpToOnChainTimelockFragment()
    }

    private fun openAddSoftwareSignerScreen() {
        val primaryKeyFlow =
            if (args.walletId?.isNotEmpty() == true) KeyFlow.REPLACE_KEY_IN_FREE_WALLET else args.keyFlow
        navigator.openAddSoftwareSignerScreen(
            activityContext = requireActivity(),
            keyFlow = primaryKeyFlow,
            groupId = args.groupId.orEmpty(),
            walletId = args.walletId.orEmpty(),
        )
        popUpToOnChainTimelockFragment()
    }

    private fun navigateToSetupTapSigner() {
        if (args.onChainAddSignerParam != null) {
            nfcSetupLauncher.launch(
                NfcSetupActivity.buildIntent(
                    activity = requireActivity(),
                    setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                    walletId = args.walletId.orEmpty(),
                    groupId = args.groupId.orEmpty(),
                    fromMembershipFlow = true,
                    onChainAddSignerParam = args.onChainAddSignerParam,
                )
            )
        } else {
            startActivity(
                NfcSetupActivity.buildIntent(
                    activity = requireActivity(),
                    setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                    walletId = args.walletId.orEmpty(),
                    groupId = args.groupId.orEmpty(),
                    fromMembershipFlow = false,
                    onChainAddSignerParam = null,
                )
            )
            popUpToOnChainTimelockFragment()
        }
    }

    private fun popUpToOnChainTimelockFragment() {
        if (args.onChainAddSignerParam != null) {
            val targetFragmentId = if (args.groupId.isNullOrEmpty()) {
                R.id.onChainTimelockAddKeyListFragment
            } else {
                R.id.onChainTimelockByzantineAddKeyFragment
            }
            findNavController().popBackStack(targetFragmentId, false)
        } else {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object Companion {
        const val REQUEST_KEY = "SignerIntroFragment"
        const val EXTRA_IS_FROM_NFC_SETUP = "EXTRA_IS_FROM_NFC_SETUP"
    }
}

