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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.text.bold
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.pullrefresh.PullRefreshIndicator
import com.nunchuk.android.compose.pullrefresh.pullRefresh
import com.nunchuk.android.compose.pullrefresh.rememberPullRefreshState
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyOnChainData
import com.nunchuk.android.main.membership.model.getButtonText
import com.nunchuk.android.main.membership.model.getLabel
import com.nunchuk.android.main.membership.model.resId
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isFacilitatorAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val addTapSignerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)?.let { signerModel ->
                    viewModel.addExistingTapSignerKey(
                        signerModel,
                        currentKeyData,
                        (activity as MembershipActivity).walletId
                    )
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
                    onConfigTimelockClicked = {
                        findNavController().navigate(
                            OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToOnChainSetUpTimelockFragment(
                                groupId = args.groupId
                            )
                        )
                    }
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

                    else -> {
                        val selectedSignerTag = selectedSignerTag
                        if (signer.type == SignerType.AIRGAP && signer.tags.isEmpty() && selectedSignerTag != null) {
                            viewModel.onUpdateSignerTag(signer, selectedSignerTag)
                        } else if (signer.type == SignerType.SOFTWARE && viewModel.isUnBackedUpSigner(
                                signer
                            )
                        ) {
                            showUnBackedUpSignerWarning()
                        } else {
                            viewModel.handleSignerNewIndex(signer.toSingleSigner())
                        }
                    }
                }
            } else {
                handleSignerTypeLogic(data.type, null)
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }

        setFragmentResultListener("ImportantNoticePassphraseFragment") { _, bundle ->
            val filteredSigners =
                bundle.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            if (!filteredSigners.isNullOrEmpty()) {
                findNavController().navigate(
                    OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToTapSignerListBottomSheetFragment(
                        filteredSigners.toTypedArray(),
                        if (filteredSigners.first().type == SignerType.COLDCARD_NFC || filteredSigners.first().tags.contains(
                                SignerTag.COLDCARD
                            )
                        ) {
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
            val filteredSigners =
                bundle.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            val signerTag = bundle.getSerializable(GlobalResultKey.EXTRA_SIGNER_TAG) as? SignerTag
            
            when {
                !filteredSigners.isNullOrEmpty() -> {
                    findNavController().navigate(
                        OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToTapSignerListBottomSheetFragment(
                            filteredSigners.toTypedArray(),
                            if (filteredSigners.first().type == SignerType.COLDCARD_NFC || filteredSigners.first().tags.contains(
                                    SignerTag.COLDCARD
                                )
                            ) {
                                SignerType.COLDCARD_NFC
                            } else {
                                filteredSigners.first().type
                            },
                            "",
                            true
                        )
                    )
                }
                signerTag != null -> {
                    handleHardwareSignerTag(signerTag)
                }
            }
            clearFragmentResult("SignerIntroFragment")
        }
        
        setFragmentResultListener("ColdCardIntroFragment") { _, bundle ->
            val signerTag = bundle.getSerializable(GlobalResultKey.EXTRA_SIGNER_TAG) as? SignerTag
            if (signerTag != null) {
                handleHardwareSignerTag(signerTag)
            }
            clearFragmentResult("ColdCardIntroFragment")
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

    private fun handleHardwareSignerTag(tag: SignerTag) {
        selectedSignerTag = tag
        val hardwareSigners = viewModel.getHardwareSigners(tag)
        if (hardwareSigners.isNotEmpty()) {
            findNavController().navigate(
                OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToTapSignerListBottomSheetFragment(
                    hardwareSigners.toTypedArray(),
                    SignerType.HARDWARE,
                    "",
                    true
                )
            )
        } else {
            openRequestAddDesktopKey(tag)
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
                OnChainTimelockByzantineAddKeyListEvent.SelectAirgapType -> {}
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
                    handleSignerTypeLogic(event.type, event.tag)
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
        // Get the actual next step to determine what UI to show
        val nextStep = data.getNextStepToAdd() ?: data.type

        when (nextStep) {
            MembershipStep.ADD_SEVER_KEY -> {
                if (!isKeyHolderLimited) {
                    navigator.openConfigGroupServerKeyActivity(
                        activityContext = requireActivity(),
                        groupStep = MembershipStage.NONE,
                        groupId = args.groupId
                    )
                }
            }
            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_TIMELOCK, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1_TIMELOCK,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0_TIMELOCK, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1_TIMELOCK -> handleHardwareKeyAdd(data)
            else -> Unit
        }
    }

    private fun handleHardwareKeyAdd(data: AddKeyOnChainData) {
        currentKeyData = data
        // Get the next step to determine what to add
        val nextStep = data.getNextStepToAdd() ?: data.type
        val allSigners = data.getAllSigners()

        if (allSigners.isEmpty()) {
            // No signers exist, check if this is inheritance key or hardware key
            if (nextStep == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY ||
                nextStep == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_TIMELOCK ||
                nextStep == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 ||
                nextStep == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1_TIMELOCK
            ) {
                // For inheritance key, navigate to inheritance intro screen
                findNavController().navigate(
                    OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToInheritanceKeyIntroFragment(
                        inheritanceType = com.nunchuk.android.main.membership.plantype.InheritancePlanType.ON_CHAIN
                    )
                )
            } else {
                // For hardware keys, open signer intro fragment
                findNavController().navigate(
                    OnChainTimelockByzantineAddKeyFragmentDirections.actionOnChainTimelockByzantineAddKeyFragmentToSignerIntroFragment(
                        walletId = (activity as MembershipActivity).walletId,
                        groupId = args.groupId,
                        supportedSigners = null,
                        keyFlow = 0,
                        onChainAddSignerParam = OnChainAddSignerParam(
                            flags = OnChainAddSignerParam.FLAG_ADD_SIGNER,
                            keyIndex = allSigners.size,
                            currentSigner = allSigners.firstOrNull()
                        )
                    )
                )
            }
        } else {
            val firstSigner = allSigners.first()

            // Special handling for TapSigner (SignerType.NFC) to add second signer for Acct 1
            if (firstSigner.type == SignerType.NFC && allSigners.size == 1) {
                viewModel.handleTapSignerAcct1Addition(
                    data,
                    firstSigner,
                    (activity as MembershipActivity).walletId
                )
                return
            }

            // Signers exist, delegate to ViewModel to handle the logic
            viewModel.handleSignerIndexCheck(
                data,
                firstSigner,
                (activity as MembershipActivity).walletId
            )
        }
    }

    private fun handleSignerTypeLogic(type: SignerType, tag: SignerTag?) {
        when (type) {
            SignerType.NFC -> openSetupTapSigner()

            SignerType.PORTAL_NFC -> openSetupPortal()

            SignerType.AIRGAP -> {
                selectedSignerTag = tag
                if (selectedSignerTag == SignerTag.COLDCARD) {
                    openSetupColdCard()
                    return
                }
                handleSelectAddAirgapType(tag)
            }

            SignerType.HARDWARE -> {
                selectedSignerTag = tag
                when (tag) {
                    SignerTag.LEDGER -> openRequestAddDesktopKey(SignerTag.LEDGER)
                    SignerTag.TREZOR -> openRequestAddDesktopKey(SignerTag.TREZOR)
                    SignerTag.BITBOX -> openRequestAddDesktopKey(SignerTag.BITBOX)
                    SignerTag.COLDCARD -> openRequestAddDesktopKey(SignerTag.COLDCARD)
                    SignerTag.JADE -> openRequestAddDesktopKey(SignerTag.JADE)
                    else -> {}
                }
            }

            else -> {}
        }
    }

    private fun openSetupColdCard() {
        val nextStep = currentKeyData?.getNextStepToAdd() ?: currentKeyData?.type
        navigator.openSetupMk4(
            activity = requireActivity(),
            args = SetupMk4Args(
                fromMembershipFlow = true,
                groupId = (activity as MembershipActivity).groupId,
                walletId = (activity as MembershipActivity).walletId,
                onChainAddSignerParam = OnChainAddSignerParam(
                    flags = if (nextStep?.isAddInheritanceKey == true) OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER else OnChainAddSignerParam.FLAG_ADD_SIGNER,
                    keyIndex = currentKeyData?.getAllSigners()?.size ?: 0,
                    currentSigner = currentKeyData?.getAllSigners()?.firstOrNull()
                )
            )
        )
    }


    private fun openVerifyTapSigner(event: OnChainTimelockByzantineAddKeyListEvent.OnVerifySigner) {
        navigator.openVerifyBackupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            backUpFilePath = event.filePath,
            masterSignerId = event.signer.id,
            groupId = args.groupId,
            walletId = (activity as MembershipActivity).walletId,
            isOnChainBackUp = true,
        )
    }

    private fun openVerifyColdCard(event: OnChainTimelockByzantineAddKeyListEvent.OnVerifySigner) {
        navigator.openBackUpSeedPhraseActivity(
            requireActivity(),
            BackUpSeedPhraseArgs(
                type = BackUpSeedPhraseType.INTRO,
                signer = event.signer,
                groupId = (activity as MembershipActivity).groupId,
                walletId = (activity as MembershipActivity).walletId
            )
        )
    }

    private fun openSetupTapSigner() {
        val nextStep = currentKeyData?.getNextStepToAdd() ?: currentKeyData?.type
        addTapSignerLauncher.launch(
            NfcSetupActivity.buildIntent(
                activity = requireActivity(),
                setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                fromMembershipFlow = true,
                groupId = args.groupId,
                walletId = (activity as MembershipActivity).walletId,
                onChainAddSignerParam = OnChainAddSignerParam(
                    flags = if (nextStep?.isAddInheritanceKey == true) OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER else OnChainAddSignerParam.FLAG_ADD_SIGNER,
                    keyIndex = currentKeyData?.getAllSigners()?.size ?: 0,
                    currentSigner = currentKeyData?.getAllSigners()?.firstOrNull()
                )
            )
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
    onConfigTimelockClicked: (AddKeyOnChainData) -> Unit = {}
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

    OnChainTimelockByzantineAddKeyListContent(
        onContinueClicked = viewModel::onContinueClicked,
        onAddClicked = viewModel::onAddKeyClicked,
        onVerifyClicked = viewModel::onVerifyClicked,
        keys = keys,
        remainingTime = remainingTime,
        onMoreClicked = onMoreClicked,
        refresh = viewModel::refresh,
        isRefreshing = state.isRefreshing,
        isAddOnly = isAddOnly,
        onConfigTimelockClicked = onConfigTimelockClicked,
        onChangeTimelockClicked = onConfigTimelockClicked
    )
}

@Composable
fun OnChainTimelockByzantineAddKeyListContent(
    isRefreshing: Boolean = false,
    remainingTime: Int,
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onConfigTimelockClicked: (data: AddKeyOnChainData) -> Unit = {},
    keys: List<AddKeyOnChainData> = emptyList(),
    onVerifyClicked: (data: AddKeyOnChainData) -> Unit = {},
    onAddClicked: (data: AddKeyOnChainData) -> Unit = {},
    refresh: () -> Unit = { },
    isAddOnly: Boolean = false,
    onChangeTimelockClicked: (data: AddKeyOnChainData) -> Unit = {}
) {
    val state = rememberPullRefreshState(isRefreshing, refresh)

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    },
                )
            },
            bottomBar = {
                if (!isAddOnly) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClicked,
                        enabled = keys.filter { it.type.isAddInheritanceKey }.all { data ->
                            data.steps.all { step ->
                                data.stepDataMap[step]?.isComplete == true
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            },
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pullRefresh(state)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.nc_let_add_your_keys),
                            style = NunchukTheme.typography.heading
                        )
                        Text(
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                                    append("Before the timelock")
                                }
                                append(
                                    ", spending requires signatures based on your wallet configuration."
                                )
                            },
                            style = NunchukTheme.typography.body
                        )
                    }

                    items(keys.filter { it.type != MembershipStep.TIMELOCK }) { key ->
                        AddKeyCard(
                            item = key,
                            onAddClicked = onAddClicked,
                            onVerifyClicked = onVerifyClicked,
                            onChangeTimelockClicked = onChangeTimelockClicked
                        )
                    }

                    item {
                        Text(
                            modifier = Modifier
                                .padding(top = 16.dp, bottom = 4.dp)
                                .fillMaxWidth(),
                            text = "Pull to refresh the key statuses.",
                            style = NunchukTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }

                    val timelockKey = keys.firstOrNull { it.type == MembershipStep.TIMELOCK }
                    if (timelockKey != null) {
                        item {
                            Text(
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                                        append("After the timelock")
                                    }
                                    append(
                                        ", spending requirements change based on your wallet configuration."
                                    )
                                },
                                style = NunchukTheme.typography.body
                            )
                            Column {
                                AddKeyCard(
                                    item = timelockKey,
                                    onAddClicked = {
                                        onConfigTimelockClicked(it)
                                    },
                                    onVerifyClicked = onVerifyClicked,
                                    onChangeTimelockClicked = onChangeTimelockClicked
                                )
                            }
                        }
                    }
                }

                PullRefreshIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun AddKeyCard(
    item: AddKeyOnChainData,
    modifier: Modifier = Modifier,
    onAddClicked: (data: AddKeyOnChainData) -> Unit = {},
    onVerifyClicked: (data: AddKeyOnChainData) -> Unit = {},
    isDisabled: Boolean = false,
    isStandard: Boolean = false,
    onChangeTimelockClicked: (data: AddKeyOnChainData) -> Unit = {}
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (banner, content) = createRefs()
        val signers = item.signers ?: emptyList()
        Box(
            modifier = modifier
                .constrainAs(content) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }) {
            if (signers.isNotEmpty()) {
                val shouldShowDashLine = signers.size == 1
                NcDashLineBox(
                    modifier = modifier,
                    showDashedBorder = shouldShowDashLine
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when {
                                    signers.size == 1 -> colorResource(id = R.color.nc_fill_slime).copy(
                                        alpha = 0.4f
                                    )

                                    item.verifyType != VerifyType.NONE -> colorResource(id = R.color.nc_fill_slime)
                                    isDisabled -> colorResource(id = R.color.nc_grey_dark_color)
                                    else -> colorResource(id = R.color.nc_fill_beewax)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NcCircleImage(
                                resId = signers.firstOrNull()?.toReadableDrawableResId()
                                    ?: R.drawable.ic_hardware_key,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = signers.firstOrNull()?.name ?: "Unknown Signer",
                                    style = NunchukTheme.typography.body
                                )

                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    NcTag(
                                        label = signers.firstOrNull()
                                            ?.toReadableSignerType(context = LocalContext.current)
                                            ?: "Unknown",
                                        backgroundColor = colorResource(
                                            id = R.color.nc_bg_mid_gray
                                        ),
                                    )
                                    val firstAcctLabel = signers.firstOrNull()?.let { firstSigner ->
                                        if (firstSigner.isShowAcctX(true)) {
                                            stringResource(R.string.nc_acct_x, firstSigner.index)
                                        } else {
                                            "Acct X"
                                        }
                                    } ?: "Acct X"
                                    NcTag(
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .alpha(if (firstAcctLabel == "Acct X") 0.5f else 1.0f),
                                        label = firstAcctLabel,
                                        backgroundColor = colorResource(
                                            id = R.color.nc_bg_mid_gray
                                        ),
                                    )
                                    val secondAcctLabel =
                                        signers.getOrNull(1)?.let { secondSigner ->
                                            if (secondSigner.isShowAcctX(true)) {
                                                stringResource(
                                                    R.string.nc_acct_x,
                                                    secondSigner.index
                                                )
                                            } else {
                                                "Acct Y"
                                            }
                                        } ?: "Acct Y"
                                    NcTag(
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .alpha(if (secondAcctLabel == "Acct Y") 0.5f else 1.0f),
                                        label = secondAcctLabel,
                                        backgroundColor = colorResource(
                                            id = R.color.nc_bg_mid_gray
                                        ),
                                    )
                                }

                                Text(
                                    modifier = Modifier.padding(top = 4.dp),
                                    text = signers.firstOrNull()?.getXfpOrCardIdLabel() ?: "",
                                    style = NunchukTheme.typography.bodySmall
                                )
                            }
                            if (signers.size >= 2) {
                                if (item.verifyType != VerifyType.NONE) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.nc_circle_checked),
                                        contentDescription = "Checked icon"
                                    )
                                    Text(
                                        modifier = Modifier.padding(start = 4.dp),
                                        style = NunchukTheme.typography.body,
                                        text = stringResource(
                                            R.string.nc_added
                                        )
                                    )
                                } else if (signers.any { it.isVisible }) {
                                    NcOutlineButton(
                                        modifier = Modifier.height(36.dp),
                                        onClick = { onVerifyClicked(item) },
                                    ) {
                                        Text(text = stringResource(R.string.nc_verify_backup))
                                    }
                                }
                            } else {
                                NcOutlineButton(
                                    modifier = Modifier.height(36.dp),
                                    enabled = isDisabled.not(),
                                    onClick = { onAddClicked(item) },
                                ) {
                                    Text(
                                        text = item.type.getButtonText(LocalContext.current),
                                        style = NunchukTheme.typography.caption,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (item.verifyType != VerifyType.NONE) {
                    Box(
                        modifier = modifier
                            .background(
                                colorResource(id = R.color.nc_fill_slime),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        ConfigItem(item, isDisabled = isDisabled, onChangeTimelockClicked = onChangeTimelockClicked)
                    }
                } else {
                    NcDashLineBox(modifier = modifier) {
                        ConfigItem(
                            item = item,
                            onAddClicked = onAddClicked,
                            isDisabled = isDisabled,
                            isStandard = isStandard,
                            onChangeTimelockClicked = onChangeTimelockClicked
                        )
                    }
                }
            }
        }

        if (item.type.isAddInheritanceKey) {
            Image(
                modifier = Modifier
                    .constrainAs(banner) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    },
                contentDescription = "Inheritance icon",
                painter = painterResource(id = R.drawable.ic_badge_inheritance)
            )
        }
    }
}

@Composable
private fun ConfigItem(
    item: AddKeyOnChainData,
    onAddClicked: ((data: AddKeyOnChainData) -> Unit)? = null,
    isDisabled: Boolean = false,
    isStandard: Boolean = false,
    onChangeTimelockClicked: (data: AddKeyOnChainData) -> Unit = {}
) {
    val signers = item.signers ?: emptyList()
    
    // Check if this is a TIMELOCK step with timelock data configured
    val isTimelockWithData = item.type == MembershipStep.TIMELOCK && 
                             item.stepDataMap[MembershipStep.TIMELOCK]?.timelock.orDefault(0) > 0
    
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcCircleImage(resId = item.type.resId)
        Column(
            modifier = Modifier
                .weight(1.0f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = item.type.getLabel(context = LocalContext.current, isStandard = isStandard),
                style = NunchukTheme.typography.body
            )
            if (item.shouldShowAcctXBadge()) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    val firstAcctLabel = signers.firstOrNull()?.let { firstSigner ->
                        if (firstSigner.isShowAcctX(true)) {
                            stringResource(R.string.nc_acct_x, firstSigner.index)
                        } else {
                            "Acct X"
                        }
                    } ?: "Acct X"
                    NcTag(
                        modifier = Modifier.alpha(if (firstAcctLabel == "Acct X") 0.5f else 1.0f),
                        label = firstAcctLabel,
                        backgroundColor = colorResource(
                            id = R.color.nc_bg_mid_gray
                        ),
                    )
                    val secondAcctLabel = signers.getOrNull(1)?.let { secondSigner ->
                        if (secondSigner.isShowAcctX(true)) {
                            stringResource(R.string.nc_acct_x, secondSigner.index)
                        } else {
                            "Acct Y"
                        }
                    } ?: "Acct Y"
                    NcTag(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .alpha(if (secondAcctLabel == "Acct Y") 0.5f else 1.0f),
                        label = secondAcctLabel,
                        backgroundColor = colorResource(
                            id = R.color.nc_bg_mid_gray
                        ),
                    )
                }
            }
            if (isTimelockWithData) {
                val timelockValue = item.stepDataMap[MembershipStep.TIMELOCK]?.timelock
                val formattedDate = timelockValue?.let {
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
                    dateFormat.format(Date(it * 1000)) // Convert seconds to milliseconds
                } ?: ""
                Text(
                    text = formattedDate,
                    style = NunchukTheme.typography.body
                )
            }
        }
        if (onAddClicked != null) {
            NcOutlineButton(
                modifier = Modifier.height(36.dp),
                enabled = isDisabled.not(),
                onClick = { onAddClicked(item) },
            ) {
                Text(
                    text = item.type.getButtonText(LocalContext.current),
                    style = NunchukTheme.typography.caption,
                )
            }
        } else if (isTimelockWithData) {
            // Show "Change" button for configured timelock
            NcOutlineButton(
                modifier = Modifier.height(36.dp),
                enabled = isDisabled.not(),
                onClick = { onChangeTimelockClicked(item) },
            ) {
                Text(
                    text = "Change",
                    style = NunchukTheme.typography.caption,
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.nc_circle_checked),
                contentDescription = "Checked icon"
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                style = NunchukTheme.typography.body,
                text = stringResource(R.string.nc_configured)
            )
        }
    }
}

