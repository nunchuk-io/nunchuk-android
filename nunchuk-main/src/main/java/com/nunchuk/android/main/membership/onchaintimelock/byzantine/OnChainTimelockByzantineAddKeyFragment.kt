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

package com.nunchuk.android.main.membership.onchaintimelock.byzantine

import android.app.Activity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.text.bold
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isAirgapTag
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.byzantine.addKey.AddByzantineKeyListContent
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyOnChainData
import com.nunchuk.android.main.membership.plantype.InheritancePlanType
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isFacilitatorAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnChainTimelockByzantineAddKeyFragment : MembershipFragment(), BottomSheetOptionListener {

    private val viewModel by viewModels<OnChainTimelockByzantineAddKeyViewModel>()

    private val args: OnChainTimelockByzantineAddKeyFragmentArgs by navArgs()

    private var selectedSignerTag: SignerTag? = null
    private var currentKeyData: AddKeyOnChainData? = null

    private val isKeyHolderLimited: Boolean by lazy { args.role.toRole == AssistedWalletRole.KEYHOLDER_LIMITED }

    private val addPortalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.handleSignerNewIndex(it)
                }
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                OnChainTimelockByzantineAddKeyListScreen(
                    viewModel = viewModel,
                    isAddOnly = args.isAddOnly,
                    membershipStepManager = membershipStepManager,
                    role = args.role.toRole,
                    onMoreClicked = ::handleShowMore,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        
        setFragmentResultListener(CustomKeyAccountFragment.REQUEST_KEY) { _, bundle ->
            val signer = bundle.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)
            val newIndex = bundle.getInt(GlobalResultKey.EXTRA_INDEX, -1)
            
            // Check if this is a result with newIndex (OnChainAddSignerParam case)
            if (newIndex != -1 && signer?.masterFingerprint?.isNotEmpty() == true) {
                viewModel.handleCustomKeyAccountResult(signer.masterFingerprint, newIndex)
            } else if (signer != null) {
                // Original flow for non-OnChainAddSignerParam case
                viewModel.handleSignerNewIndex(signer)
            }
            clearFragmentResult(CustomKeyAccountFragment.REQUEST_KEY)
        }
        
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val data = TapSignerListBottomSheetFragmentArgs.fromBundle(bundle)
            if (data.signers.isNotEmpty()) {
                val signer = data.signers.first()
                when (data.type) {
                    SignerType.NFC -> viewModel.addExistingTapSignerKey(
                        signer,
                        currentKeyData,
                        (activity as MembershipActivity).walletId
                    )
                    SignerType.PORTAL_NFC -> findNavController().navigate(
                        OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToCustomKeyAccountFragmentFragment(
                            signer,
                            groupId = args.groupId,
                            walletId = (activity as MembershipActivity).walletId,
                        )
                    )
                    SignerType.AIRGAP -> {
                        val hasTag = signer.tags.any { it.isAirgapTag || it == SignerTag.COLDCARD }
                        val selectedSignerTag = selectedSignerTag
                        if (hasTag || selectedSignerTag == null) {
                            findNavController().navigate(
                                OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToCustomKeyAccountFragmentFragment(
                                    signer,
                                    groupId = args.groupId,
                                    walletId = (activity as MembershipActivity).walletId,
                                )
                            )
                        } else {
                            viewModel.onUpdateSignerTag(signer, selectedSignerTag)
                        }
                    }
                    else -> {
                        val selectedSignerTag = selectedSignerTag
                        if (signer.type == SignerType.AIRGAP && signer.tags.isEmpty() && selectedSignerTag != null) {
                            viewModel.onUpdateSignerTag(signer, selectedSignerTag)
                        } else if (signer.type == SignerType.SOFTWARE && viewModel.isUnBackedUpSigner(signer)) {
                            showUnBackedUpSignerWarning()
                        } else {
                            viewModel.handleSignerNewIndex(signer.toSingleSigner())
                        }
                    }
                }
            } else {
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.PORTAL_NFC -> openSetupPortal()
                    SignerType.AIRGAP -> handleSelectAddAirgapType(selectedSignerTag)
                    SignerType.COLDCARD_NFC -> {
                        navigator.openSetupMk4(
                            activity = requireActivity(),
                            args = SetupMk4Args(
                                fromMembershipFlow = true,
                                groupId = args.groupId,
                                walletId = (activity as MembershipActivity).walletId,
                                onChainAddSignerParam = OnChainAddSignerParam(
                                    flags = OnChainAddSignerParam.FLAG_ADD_SIGNER,
                                    keyIndex = currentKeyData?.signers?.size ?: 0
                                )
                            )
                        )
                    }
                    SignerType.HARDWARE -> selectedSignerTag?.let { openRequestAddDesktopKey(it) }
                    SignerType.SOFTWARE -> openAddSoftwareKey()
                    else -> throw IllegalArgumentException("Signer type invalid ${data.type}")
                }
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }
        
        setFragmentResultListener("ImportantNoticePassphraseFragment") { _, bundle ->
            val filteredSigners = bundle.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            if (!filteredSigners.isNullOrEmpty()) {
                findNavController().navigate(
                    OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToTapSignerListBottomSheetFragment(
                        filteredSigners.toTypedArray(),
                        if (filteredSigners.first().type == SignerType.COLDCARD_NFC || filteredSigners.first().tags.contains(SignerTag.COLDCARD)) {
                            SignerType.COLDCARD_NFC
                        } else {
                            SignerType.AIRGAP
                        },
                        "",
                        true
                    )
                )
            }
            clearFragmentResult("ImportantNoticePassphraseFragment")
        }
        setFragmentResultListener("SignerIntroFragment") { _, bundle ->
            val filteredSigners = bundle.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            if (!filteredSigners.isNullOrEmpty()) {
                findNavController().navigate(
                    OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToTapSignerListBottomSheetFragment(
                        filteredSigners.toTypedArray(),
                        if (filteredSigners.first().type == SignerType.COLDCARD_NFC || filteredSigners.first().tags.contains(SignerTag.COLDCARD)) {
                            SignerType.COLDCARD_NFC
                        } else {
                            filteredSigners.first().type
                        },
                        "",
                        true
                    )
                )
            }
            clearFragmentResult("SignerIntroFragment")
        }

        if (args.role.toRole.isFacilitatorAdmin) {
            showFacilitatorInfoDialog()
        }

        // Setup TapSigner caching with MembershipActivity
        val membershipActivity = activity as? MembershipActivity
        membershipActivity?.setTapSignerCachingCallback { isoDep, cvc ->
            viewModel.cacheTapSignerXpub(isoDep, cvc)
        }

        // Observe requestCacheTapSignerXpubEvent state to handle TapSigner caching
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state.requestCacheTapSignerXpubEvent) {
                    membershipActivity?.requestTapSignerCaching()
                    viewModel.resetRequestCacheTapSignerXpub()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear TapSigner caching callback when fragment is destroyed
        val membershipActivity = activity as? MembershipActivity
        membershipActivity?.clearTapSignerCachingCallback()
    }

    private fun showFacilitatorInfoDialog() {
        NCInfoDialog(requireActivity())
            .showDialog(message = getString(R.string.nc_info_facilitator))
    }

    private fun checkTwoSoftwareKeySameDevice(onSuccess: () -> Unit) {
        val total = viewModel.getCountWalletSoftwareSignersInDevice()
        if (total >= 1) {
            NCInfoDialog(requireActivity())
                .showDialog(
                    title = getString(R.string.nc_text_warning),
                    btnYes = getString(R.string.nc_text_continue),
                    message = SpannableStringBuilder()
                        .bold {
                            append(getString(R.string.nc_info_software_key_same_device_part_1))
                        }
                        .append(" ")
                        .append(getString(R.string.nc_info_software_key_same_device_part_2)),
                    onYesClick = { onSuccess() },
                    btnInfo = getString(R.string.nc_i_ll_choose_another_type_of_key),
                )
        } else {
            onSuccess()
        }
    }

    private fun openAddSoftwareKey() {
        navigator.openAddSoftwareSignerScreen(
            activityContext = requireActivity(),
            groupId = args.groupId,
        )
    }

    private fun getSignerTag(type: Int): SignerTag? {
        return when (type) {
            SheetOptionType.TYPE_ADD_AIRGAP_JADE -> SignerTag.JADE
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER -> SignerTag.SEEDSIGNER
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT -> SignerTag.PASSPORT
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE -> SignerTag.KEYSTONE
            else -> null
        }
    }

    private fun openRequestAddDesktopKey(tag: SignerTag) {
        membershipStepManager.currentStep?.let { step ->
            findNavController().navigate(
                OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToAddDesktopKeyFragment(
                    tag,
                    step,
                    args.groupId
                )
            )
        }
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            args = AddAirSignerArgs(
                isMembershipFlow = true,
                tag = tag,
                groupId = args.groupId,
                step = membershipStepManager.currentStep,
            )
        )
    }

    private fun showAddColdcardOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_NFC,
                    label = getString(R.string.nc_add_coldcard_via_nfc),
                    resId = R.drawable.ic_nfc_indicator_small
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_QR,
                    label = getString(R.string.nc_add_coldcard_via_qr),
                    resId = R.drawable.ic_qr
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_USB,
                    label = getString(R.string.nc_add_coldcard_via_usb),
                    resId = R.drawable.ic_usb
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_FILE,
                    label = getString(R.string.nc_add_coldcard_via_file),
                    resId = R.drawable.ic_import
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showAirgapOptions() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_what_type_of_airgap_you_have),
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_JADE,
                    label = getString(R.string.nc_blockstream_jade),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
                    label = getString(R.string.nc_foudation_passport),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
                    label = getString(R.string.nc_seedsigner),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
                    label = getString(R.string.nc_keystone),
                ),
                SheetOption(
                    type = SignerType.COLDCARD_NFC.ordinal,
                    label = getString(R.string.nc_coldcard)
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun observer() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is OnChainTimelockByzantineAddKeyListEvent.OnAddKey -> handleOnAddKey(event.data)
                is OnChainTimelockByzantineAddKeyListEvent.OnVerifySigner -> {
                    if (event.signer.type == SignerType.NFC) {
                        openVerifyTapSigner(event)
                    } else {
                        openVerifyColdCard(event)
                    }
                }
                OnChainTimelockByzantineAddKeyListEvent.OnAddAllKey -> findNavController().popBackStack()
                is OnChainTimelockByzantineAddKeyListEvent.ShowError -> showError(event.message)
                OnChainTimelockByzantineAddKeyListEvent.SelectAirgapType -> showAirgapOptions()
                is OnChainTimelockByzantineAddKeyListEvent.UpdateSignerTag -> findNavController().navigate(
                    OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToCustomKeyAccountFragmentFragment(
                        event.signer,
                        groupId = args.groupId,
                        walletId = (activity as MembershipActivity).walletId,
                    )
                )
                is OnChainTimelockByzantineAddKeyListEvent.NavigateToCustomKeyAccount -> {
                    findNavController().navigate(
                        OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToCustomKeyAccountFragmentFragment(
                            event.signer,
                            groupId = args.groupId,
                            walletId = event.walletId,
                            onChainAddSignerParam = event.onChainAddSignerParam
                        )
                    )
                }
                is OnChainTimelockByzantineAddKeyListEvent.HandleSignerTypeLogic -> {
                    handleSignerTypeLogic(event.signer)
                }
            }
        }
        flowObserver(viewModel.state) {
            if (it.shouldShowKeyAdded) {
                findNavController().navigate(
                    OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToKeyAddedToGroupWalletFragment()
                )
                viewModel.markHandledShowKeyAdded()
            }
        }
    }

    private fun handleOnAddKey(data: AddKeyOnChainData) {
        currentKeyData = data
        when (data.type) {
            MembershipStep.ADD_SEVER_KEY -> {
                if (!isKeyHolderLimited) {
                    navigator.openConfigGroupServerKeyActivity(
                        activityContext = requireActivity(),
                        groupStep = MembershipStage.NONE,
                        groupId = args.groupId
                    )
                }
            }

            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY, 
            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 -> {
                // For Byzantine on-chain timelock inheritance keys
                findNavController().navigate(
                    OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToInheritanceKeyIntroFragment(
                        inheritanceType = InheritancePlanType.ON_CHAIN
                    )
                )
            }

            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4,
            -> handleHardwareKeyAdd(data)

            else -> Unit
        }
    }

    private fun handleHardwareKeyAdd(data: AddKeyOnChainData) {
        if (data.signers.isNullOrEmpty()) {
            // No signers exist, open signer intro fragment
            findNavController().navigate(
                OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToSignerIntroFragment(
                    walletId = (activity as MembershipActivity).walletId,
                    groupId = args.groupId,
                    supportedSigners = null,
                    keyFlow = 0,
                    onChainAddSignerParam = OnChainAddSignerParam(
                        flags = OnChainAddSignerParam.FLAG_ADD_SIGNER,
                        keyIndex = data.signers?.size ?: 0,
                        currentSignerXfp = data.signers?.firstOrNull()?.fingerPrint ?: ""
                    )
                )
            )
        } else {
            val firstSigner = data.signers.first()

            // Special handling for TapSigner (SignerType.NFC) to add second signer for Acct 1
            if (firstSigner.type == SignerType.NFC && data.signers.size == 1) {
                viewModel.handleTapSignerAcct1Addition(data, firstSigner, (activity as MembershipActivity).walletId)
                return
            }

            // Signers exist, delegate to ViewModel to handle the logic
            viewModel.handleSignerIndexCheck(data, firstSigner, (activity as MembershipActivity).walletId)
        }
    }

    private fun handleSignerTypeLogic(firstSigner: SignerModel) {
        when (firstSigner.type) {
            SignerType.NFC -> openSetupTapSigner()
            
            SignerType.PORTAL_NFC -> openSetupPortal()
            
            SignerType.COLDCARD_NFC -> {
                selectedSignerTag = SignerTag.COLDCARD
                navigator.openSetupMk4(
                    activity = requireActivity(),
                    args = SetupMk4Args(
                        fromMembershipFlow = true,
                        groupId = args.groupId,
                        walletId = (activity as MembershipActivity).walletId,
                        onChainAddSignerParam = OnChainAddSignerParam(
                            flags = OnChainAddSignerParam.FLAG_ADD_SIGNER,
                            keyIndex = currentKeyData?.signers?.size ?: 0,
                            currentSignerXfp = currentKeyData?.signers?.firstOrNull()?.fingerPrint ?: ""
                        )
                    )
                )
            }
            
            SignerType.AIRGAP -> {
                val tag = firstSigner.tags.firstOrNull()
                selectedSignerTag = tag
                handleSelectAddAirgapType(tag)
            }
            
            SignerType.HARDWARE -> {
                val tag = firstSigner.tags.firstOrNull()
                selectedSignerTag = tag
                when (tag) {
                    SignerTag.LEDGER -> openRequestAddDesktopKey(SignerTag.LEDGER)
                    SignerTag.TREZOR -> openRequestAddDesktopKey(SignerTag.TREZOR)
                    SignerTag.BITBOX -> openRequestAddDesktopKey(SignerTag.BITBOX)
                    SignerTag.COLDCARD -> openRequestAddDesktopKey(SignerTag.COLDCARD)
                    else -> {}
                }
            }
            
            else -> {}
        }
    }


    private fun openVerifyTapSigner(event: OnChainTimelockByzantineAddKeyListEvent.OnVerifySigner) {
        navigator.openVerifyBackupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            backUpFilePath = event.filePath,
            masterSignerId = event.signer.id,
            groupId = args.groupId,
            walletId = (activity as MembershipActivity).walletId,
        )
    }

    private fun openVerifyColdCard(event: OnChainTimelockByzantineAddKeyListEvent.OnVerifySigner) {
        navigator.openSetupMk4(
            activity = requireActivity(),
            args = SetupMk4Args(
                fromMembershipFlow = true,
                backUpFilePath = event.filePath,
                xfp = event.signer.fingerPrint,
                groupId = args.groupId,
                action = if (event.backUpFileName.isNotEmpty()) ColdcardAction.VERIFY_KEY else ColdcardAction.UPLOAD_BACKUP,
                signerType = event.signer.type,
                keyName = event.signer.name,
                backUpFileName = event.backUpFileName
            )
        )
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            groupId = args.groupId,
        )
    }

    private fun openSetupPortal() {
        navigator.openPortalScreen(
            launcher = addPortalLauncher,
            activity = requireActivity(),
            args = PortalDeviceArgs(
                type = PortalDeviceFlow.SETUP,
                isMembershipFlow = true
            ),
        )
    }

    private fun showUnBackedUpSignerWarning() {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(com.nunchuk.android.wallet.R.string.nc_unbacked_up_signer_warning_desc),
            onYesClick = {}
        )
    }
}

@Composable
fun OnChainTimelockByzantineAddKeyListScreen(
    viewModel: OnChainTimelockByzantineAddKeyViewModel = viewModel(),
    isAddOnly: Boolean = false,
    membershipStepManager: MembershipStepManager,
    onMoreClicked: () -> Unit = {},
    role: AssistedWalletRole = AssistedWalletRole.NONE,
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    
    // Convert AddKeyOnChainData to AddKeyData for display
    AddByzantineKeyListContent(
        onContinueClicked = viewModel::onContinueClicked,
        onAddClicked = { keyData -> 
            // Find the corresponding AddKeyOnChainData
            keys.find { it.type == keyData.type }?.let { viewModel.onAddKeyClicked(it) }
        },
        onVerifyClicked = { keyData -> 
            // Find the corresponding AddKeyOnChainData
            keys.find { it.type == keyData.type }?.let { viewModel.onVerifyClicked(it) }
        },
        keys = keys.map { onChainData ->
            com.nunchuk.android.main.membership.model.AddKeyData(
                type = onChainData.type,
                signer = onChainData.signers?.firstOrNull(),
                verifyType = onChainData.verifyType
            )
        },
        missingBackupKeys = state.missingBackupKeys.map { onChainData ->
            com.nunchuk.android.main.membership.model.AddKeyData(
                type = onChainData.type,
                signer = onChainData.signers?.firstOrNull(),
                verifyType = onChainData.verifyType
            )
        },
        remainingTime = remainingTime,
        onMoreClicked = onMoreClicked,
        refresh = viewModel::refresh,
        isRefreshing = state.isRefreshing,
        isAddOnly = isAddOnly,
        groupWalletType = state.groupWalletType,
        role = role
    )
}

