package com.nunchuk.android.main.membership.onchaintimelock.replacekey

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.pullrefresh.PullRefreshIndicator
import com.nunchuk.android.compose.pullrefresh.pullRefresh
import com.nunchuk.android.compose.pullrefresh.rememberPullRefreshState
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.core.util.InheritancePlanType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddReplaceKeyOnChainData
import com.nunchuk.android.main.membership.model.ReplaceStepData
import com.nunchuk.android.main.membership.model.resId
import com.nunchuk.android.main.membership.onchaintimelock.importantpassphrase.ImportantNoticePassphraseFragment
import com.nunchuk.android.main.membership.signer.OnChainSignerIntroFragment
import com.nunchuk.android.model.OnChainReplaceKeyStep
import com.nunchuk.android.model.TimelockExtra
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.isFacilitatorAdmin
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.mk4.inheritance.ColdCardIntroFragment
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
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class OnChainReplaceKeysFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: OnChainReplaceKeysViewModel by activityViewModels()

    private val args by navArgs<OnChainReplaceKeysFragmentArgs>()
    private var selectedSignerTag: SignerTag? = null
    private var currentKeyData: AddReplaceKeyOnChainData? = null

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                OnChainReplaceKeysScreen(
                    viewModel = viewModel,
                    onAddKeyClicked = ::handleOnAddKey,
                    onConfigTimelockClicked = { data ->
                        val walletTimelock = data.newTimelock ?: data.originalTimelock
                        findNavController().navigate(
                            OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToOnChainSetUpTimelockFragment(
                                groupId = args.groupId,
                                timelockExtra = TimelockExtra(
                                    value = walletTimelock?.timelockValue ?: 0L,
                                    timezone = walletTimelock?.timezone ?: ""
                                ),
                                isReplaceKeyFlow = true,
                                walletId = (activity as MembershipActivity).walletId
                            )
                        )
                    },
                    onCreateNewWalletSuccess = { walletId ->
                        findNavController().navigate(
                            OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToCreateWalletSuccessFragment(
                                walletId = walletId,
                                replacedWalletId = args.walletId
                            )
                        )
                    },
                    onVerifyClicked = viewModel::onVerifyClicked,
                    onRemove = viewModel::onRemoveKey,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()

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

                    else -> {
                        val selectedSignerTag = selectedSignerTag
                        if (signer.type == SignerType.AIRGAP && signer.tags.isEmpty() && selectedSignerTag != null) {
                            viewModel.onUpdateSignerTag(signer, selectedSignerTag)
                        } else {
                            viewModel.handleSignerNewIndex(signer.toSingleSigner())
                        }
                    }
                }
            } else {
                handleSignerTypeLogic(data.type, selectedSignerTag)
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }

        setFragmentResultListener(ImportantNoticePassphraseFragment.REQUEST_KEY) { _, bundle ->
            val filteredSigners =
                bundle.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            val signerTag = filteredSigners?.firstOrNull()?.tags?.firstOrNull()
            selectedSignerTag = signerTag
            if (!filteredSigners.isNullOrEmpty()) {
                findNavController().navigate(
                    OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToTapSignerListBottomSheetFragment(
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
            clearFragmentResult(ImportantNoticePassphraseFragment.REQUEST_KEY)
        }
        setFragmentResultListener(OnChainSignerIntroFragment.REQUEST_KEY) { _, bundle ->
            val filteredSigners =
                bundle.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            val signerModel = bundle.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)
            val requestDesktopSignerTag =
                bundle.getSerializable(GlobalResultKey.EXTRA_SIGNER_TAG) as? SignerTag
            val signerTag =
                requestDesktopSignerTag ?: filteredSigners?.firstOrNull()?.tags?.firstOrNull()
            selectedSignerTag = signerTag
            val isFromNfcSetup =
                bundle.getBoolean(OnChainSignerIntroFragment.EXTRA_IS_FROM_NFC_SETUP, false)

            when {
                isFromNfcSetup && signerModel != null -> {
                    viewModel.addExistingTapSignerKey(
                        signerModel,
                        currentKeyData,
                        (activity as MembershipActivity).walletId
                    )
                }

                !filteredSigners.isNullOrEmpty() -> {
                    findNavController().navigate(
                        OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToTapSignerListBottomSheetFragment(
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
            clearFragmentResult(OnChainSignerIntroFragment.REQUEST_KEY)
        }

        setFragmentResultListener(ColdCardIntroFragment.REQUEST_KEY) { _, bundle ->
            val signerTag = bundle.getSerializable(GlobalResultKey.EXTRA_SIGNER_TAG) as? SignerTag
            if (signerTag != null) {
                handleHardwareSignerTag(signerTag)
            }
            clearFragmentResult(ColdCardIntroFragment.REQUEST_KEY)
        }

        if (viewModel.getRole().isFacilitatorAdmin) {
            showFacilitatorInfoDialog()
        }

        // Setup TapSigner caching with MembershipActivity
        val membershipActivity = activity as? MembershipActivity
        membershipActivity?.setTapSignerCachingCallback { isoDep, cvc ->
            viewModel.cacheTapSignerXpub(isoDep, cvc)
        }

        // Observe requestCacheTapSignerXpubEvent state to handle TapSigner caching
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.requestCacheTapSignerXpubEvent) {
                    membershipActivity?.requestTapSignerCaching()
                    viewModel.resetRequestCacheTapSignerXpub()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getReplaceWalletStatus()
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

    private fun handleHardwareSignerTag(tag: SignerTag) {
        selectedSignerTag = tag
        val hardwareSigners = viewModel.getHardwareSigners(tag)
        if (hardwareSigners.isNotEmpty()) {
            findNavController().navigate(
                OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToTapSignerListBottomSheetFragment(
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
        viewModel.getCurrentStep()?.let { step ->
            findNavController().navigate(
                OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToAddDesktopKeyFragment(
                    tag,
                    step,
                    args.groupId
                )
            )
        }
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        val nextStep = currentKeyData?.getNextStepToAdd() ?: currentKeyData?.type
        val currentStep = viewModel.getCurrentStep()
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            args = AddAirSignerArgs(
                isMembershipFlow = true,
                tag = tag,
                groupId = args.groupId,
                replacedXfp = viewModel.replacedXfp,
                onChainAddSignerParam = OnChainAddSignerParam(
                    flags = if (nextStep?.isAddInheritanceKey == true) OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER else OnChainAddSignerParam.FLAG_ADD_SIGNER,
                    keyIndex = currentKeyData?.getAllSigners()?.size ?: 0,
                    currentSigner = currentKeyData?.getAllSigners()?.firstOrNull(),
                    replaceInfo = currentStep?.let {
                        OnChainAddSignerParam.ReplaceInfo(
                            replacedXfp = viewModel.replacedXfp,
                            step = it
                        )
                    }
                )
            )
        )
    }

    private fun observer() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is OnChainReplaceKeyEvent.OnAddKey -> handleOnAddKey(event.data)
                is OnChainReplaceKeyEvent.OnVerifySigner -> {
                    if (event.signer.type == SignerType.NFC) {
                        openVerifyTapSigner(event)
                    } else {
                        openVerifyColdCard(event)
                    }
                }

                OnChainReplaceKeyEvent.OnAddAllKey -> findNavController().popBackStack(
                    R.id.addGroupKeyStepFragment,
                    false
                )

                is OnChainReplaceKeyEvent.ShowError -> showError(event.message)
                OnChainReplaceKeyEvent.SelectAirgapType -> {}
                is OnChainReplaceKeyEvent.OpenUploadConfigurationScreen -> {
                    navigator.openUploadConfigurationScreen(
                        activityContext = requireActivity(),
                        walletId = event.walletId,
                        replacedWalletId = args.walletId,
                        isOnChainFlow = true,
                        groupId = args.groupId.takeIf { it.isNotEmpty() },
                        quickWalletParam = null
                    )
                }

                is OnChainReplaceKeyEvent.UpdateSignerTag -> findNavController().navigate(
                    OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToCustomKeyAccountFragmentFragment(
                        event.signer,
                        groupId = args.groupId,
                        walletId = (activity as MembershipActivity).walletId,
                    )
                )

                is OnChainReplaceKeyEvent.HandleSignerTypeLogic -> {
                    handleSignerTypeLogic(event.type, event.tag)
                }
            }
        }
    }

    private fun handleOnAddKey(data: AddReplaceKeyOnChainData) {
        currentKeyData = data
        // Get the actual next step to determine what UI to show
        val nextStep = data.getNextStepToAdd() ?: data.type
        viewModel.setCurrentStep(nextStep)
        viewModel.setReplacingXfp(data.fingerPrint)
        viewModel.initReplaceKey()
        when (nextStep) {
            OnChainReplaceKeyStep.INHERITANCE_KEY, OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK, OnChainReplaceKeyStep.INHERITANCE_KEY_1, OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK,
            OnChainReplaceKeyStep.HARDWARE_KEY, OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK, OnChainReplaceKeyStep.HARDWARE_KEY_1, OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK -> handleHardwareKeyAdd(
                data
            )

            else -> Unit
        }
    }

    private fun handleHardwareKeyAdd(data: AddReplaceKeyOnChainData) {
        currentKeyData = data
        // Get the next step to determine what to add
        val nextStep = data.getNextStepToAdd() ?: data.type
        val allSigners = data.getAllSigners()

        if (allSigners.isEmpty()) {
            // No signers exist, check if this is inheritance key or hardware key
            if (nextStep == OnChainReplaceKeyStep.INHERITANCE_KEY ||
                nextStep == OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK ||
                nextStep == OnChainReplaceKeyStep.INHERITANCE_KEY_1 ||
                nextStep == OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK
            ) {
                // For inheritance key, navigate to inheritance intro screen
                val currentStep = viewModel.getCurrentStep()
                findNavController().navigate(
                    OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToInheritanceKeyIntroFragment(
                        inheritanceType = InheritancePlanType.ON_CHAIN,
                        onChainAddSignerParam = OnChainAddSignerParam(
                            flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER,
                            keyIndex = currentKeyData?.getAllSigners()?.size ?: 0,
                            currentSigner = currentKeyData?.getAllSigners()?.firstOrNull(),
                            replaceInfo = currentStep?.let {
                                OnChainAddSignerParam.ReplaceInfo(
                                    replacedXfp = viewModel.replacedXfp,
                                    step = it
                                )
                            }
                        )
                    )
                )
            } else {
                // For hardware keys, open signer intro fragment
                val currentStep = viewModel.getCurrentStep()
                findNavController().navigate(
                    OnChainReplaceKeysFragmentDirections.actionOnChainReplaceKeysFragmentToSignerIntroFragment(
                        walletId = (activity as MembershipActivity).walletId,
                        groupId = args.groupId,
                        supportedSigners = null,
                        keyFlow = 0,
                        onChainAddSignerParam = OnChainAddSignerParam(
                            flags = OnChainAddSignerParam.FLAG_ADD_SIGNER,
                            keyIndex = allSigners.size,
                            currentSigner = allSigners.firstOrNull(),
                            replaceInfo = currentStep?.let {
                                OnChainAddSignerParam.ReplaceInfo(
                                    replacedXfp = viewModel.replacedXfp,
                                    step = it
                                )
                            }
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

            SignerType.COLDCARD_NFC -> {
                selectedSignerTag = SignerTag.COLDCARD
                openSetupColdCard()
            }

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
        selectedSignerTag = null
    }

    private fun openSetupColdCard() {
        val nextStep = currentKeyData?.getNextStepToAdd() ?: currentKeyData?.type
        val currentStep = viewModel.getCurrentStep()
        navigator.openSetupMk4(
            activity = requireActivity(),
            args = SetupMk4Args(
                fromMembershipFlow = true,
                groupId = (activity as MembershipActivity).groupId,
                walletId = (activity as MembershipActivity).walletId,
                replacedXfp = viewModel.replacedXfp,
                onChainAddSignerParam = OnChainAddSignerParam(
                    flags = if (nextStep?.isAddInheritanceKey == true) OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER else OnChainAddSignerParam.FLAG_ADD_SIGNER,
                    keyIndex = currentKeyData?.getAllSigners()?.size ?: 0,
                    currentSigner = currentKeyData?.getAllSigners()?.firstOrNull(),
                    replaceInfo = currentStep?.let {
                        OnChainAddSignerParam.ReplaceInfo(
                            replacedXfp = viewModel.replacedXfp,
                            step = it
                        )
                    }
                )
            )
        )
    }

    private fun openVerifyTapSigner(event: OnChainReplaceKeyEvent.OnVerifySigner) {
        navigator.openCreateBackUpTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            masterSignerId = event.signer.fingerPrint,
            groupId = (activity as MembershipActivity).groupId,
            walletId = (activity as MembershipActivity).walletId,
            replacedXfp = event.signer.fingerPrint,
            isOnChainBackUp = true,
        )
    }

    private fun openVerifyColdCard(event: OnChainReplaceKeyEvent.OnVerifySigner) {
        navigator.openBackUpSeedPhraseActivity(
            requireActivity(),
            BackUpSeedPhraseArgs(
                type = BackUpSeedPhraseType.INTRO,
                signer = event.signer,
                groupId = (activity as MembershipActivity).groupId,
                walletId = (activity as MembershipActivity).walletId,
                replacedXfp = event.signer.fingerPrint
            )
        )
    }

    private fun openSetupTapSigner() {
        val nextStep = currentKeyData?.getNextStepToAdd() ?: currentKeyData?.type
        val currentStep = viewModel.getCurrentStep()
        addTapSignerLauncher.launch(
            NfcSetupActivity.buildIntent(
                activity = requireActivity(),
                setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                fromMembershipFlow = true,
                replacedXfp = viewModel.replacedXfp,
                groupId = args.groupId,
                walletId = (activity as MembershipActivity).walletId,
                onChainAddSignerParam = OnChainAddSignerParam(
                    flags = if (nextStep?.isAddInheritanceKey == true) OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER else OnChainAddSignerParam.FLAG_ADD_SIGNER,
                    keyIndex = currentKeyData?.getAllSigners()?.size ?: 0,
                    currentSigner = currentKeyData?.getAllSigners()?.firstOrNull(),
                    replaceInfo = currentStep?.let {
                        OnChainAddSignerParam.ReplaceInfo(
                            replacedXfp = viewModel.replacedXfp,
                            step = it
                        )
                    }
                )
            )
        )
    }
}

@Composable
fun OnChainReplaceKeysScreen(
    viewModel: OnChainReplaceKeysViewModel,
    onAddKeyClicked: (AddReplaceKeyOnChainData) -> Unit,
    onConfigTimelockClicked: (AddReplaceKeyOnChainData) -> Unit,
    onCreateNewWalletSuccess: (String) -> Unit,
    onVerifyClicked: (AddReplaceKeyOnChainData) -> Unit,
    onRemove: (String) -> Unit
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.createWalletSuccess) {
        if (uiState.createWalletSuccess is com.nunchuk.android.model.StateEvent.String) {
            onCreateNewWalletSuccess((uiState.createWalletSuccess as com.nunchuk.android.model.StateEvent.String).data)
            viewModel.markOnCreateWalletSuccess()
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            snackState.showNunchukSnackbar(message = uiState.message, type = NcToastType.ERROR)
            viewModel.onHandledMessage()
        }
    }

    OnChainReplaceKeysContent(
        uiState = uiState,
        keys = keys,
        isEnableCreateWallet = viewModel.isEnableContinueButton(),
        snackState = snackState,
        onAddKeyClicked = onAddKeyClicked,
        onConfigTimelockClicked = onConfigTimelockClicked,
        onCreateWalletClicked = viewModel::onCreateWallet,
        onCancelReplaceWallet = viewModel::onCancelReplaceWallet,
        onVerifyClicked = onVerifyClicked,
        onRemove = onRemove,
        refresh = viewModel::getReplaceWalletStatus,
        isRefreshing = uiState.isRefreshing
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnChainReplaceKeysContent(
    uiState: OnChainReplaceKeysUiState = OnChainReplaceKeysUiState(),
    isEnableCreateWallet: Boolean = false,
    keys: List<AddReplaceKeyOnChainData> = emptyList(),
    onAddKeyClicked: (AddReplaceKeyOnChainData) -> Unit = {},
    onConfigTimelockClicked: (AddReplaceKeyOnChainData) -> Unit = {},
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onCreateWalletClicked: () -> Unit = {},
    onCancelReplaceWallet: () -> Unit = {},
    onVerifyClicked: (AddReplaceKeyOnChainData) -> Unit = {},
    onRemove: (String) -> Unit = {},
    refresh: () -> Unit = {},
    isRefreshing: Boolean = false
) {
    val pullRefreshState = rememberPullRefreshState(isRefreshing, refresh)
    var showSheetOptions by rememberSaveable { mutableStateOf(false) }
    var showConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var removeXfp by rememberSaveable { mutableStateOf("") }
    NunchukTheme {
        if (uiState.isLoading) {
            NcLoadingDialog()
        }
        NcScaffold(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize(),
            snackState = snackState,
            topBar = {
                NcTopAppBar(title = "", actions = {
                    IconButton(onClick = { showSheetOptions = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                })
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onCreateWalletClicked,
                    enabled = isEnableCreateWallet
                ) {
                    Text(text = stringResource(R.string.nc_continue_to_create_a_new_wallet))
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pullRefresh(pullRefreshState)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Replace key or change timelock",
                        style = NunchukTheme.typography.heading
                    )

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        text = buildAnnotatedString {

                            withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                                append("Before the timelock")
                            }
                            append(
                                ", spending requires two signatures from a 2-of-4 multisig:"
                            )
                        },
                        style = NunchukTheme.typography.body
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(keys.filter { it.isServerOrTimelockKey().not() }) { data ->
                            ReplaceKeyCard(
                                data = data,
                                onReplaceClicked = {
                                    onAddKeyClicked(data)
                                },
                                onRemoveClicked = {
                                    removeXfp = data.fingerPrint
                                    showRemoveDialog = true
                                },
                                onVerifyClicked = {
                                    onVerifyClicked(data)
                                }
                            )
                        }

                        item {
                            val serverKey =
                                keys.firstOrNull { it.type == OnChainReplaceKeyStep.SERVER_KEY }
                            serverKey?.let {
                                ReplaceServerCard(
                                    item = serverKey,
                                )
                            }
                        }

                        item {
                            Text(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth(),
                                text = "Pull to refresh the key statuses.",
                                style = NunchukTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }

                        item {
                            Text(
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                                text = buildAnnotatedString {

                                    withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                                        append("After the timelock")
                                    }
                                    append(
                                        ", spending only requires one signature from a 1-of-3 multisig. The keyset remains the same, minus the Platform key."
                                    )
                                },
                                style = NunchukTheme.typography.body
                            )
                            val timelockKey =
                                keys.firstOrNull { it.type == OnChainReplaceKeyStep.TIMELOCK }
                            if (timelockKey != null) {
                                TimelockReplaceCard(
                                    modifier = Modifier.padding(top = 16.dp),
                                    data = timelockKey,
                                    onChangeClicked = { onConfigTimelockClicked(timelockKey) }
                                )
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            if (showConfirmationDialog) {
                NcConfirmationDialog(
                    title = stringResource(R.string.nc_confirmation),
                    message = stringResource(R.string.nc_confirm_cancel_replacement_desc),
                    onPositiveClick = {
                        onCancelReplaceWallet()
                        showConfirmationDialog = false
                    },
                    onDismiss = {
                        showConfirmationDialog = false
                    }
                )
            }

            if (showRemoveDialog) {
                NcConfirmationDialog(
                    title = stringResource(R.string.nc_text_warning),
                    message = stringResource(R.string.nc_delete_key_msg),
                    onPositiveClick = {
                        onRemove(removeXfp)
                        showRemoveDialog = false
                    },
                    onDismiss = {
                        showRemoveDialog = false
                    }
                )
            }

            if (showSheetOptions) {
                NcSelectableBottomSheet(
                    options = listOf(
                        stringResource(R.string.nc_cancel_key_replacement),
                    ),
                    showSelectIndicator = false,
                    onSelected = {
                        showConfirmationDialog = true
                        showSheetOptions = false
                    },
                    onDismiss = {
                        showSheetOptions = false
                    }
                )
            }
        }
    }
}

@Composable
fun ReplaceKeyCard(
    data: AddReplaceKeyOnChainData,
    modifier: Modifier = Modifier,
    onReplaceClicked: (data: AddReplaceKeyOnChainData) -> Unit = {},
    onRemoveClicked: (data: AddReplaceKeyOnChainData) -> Unit = {},
    onVerifyClicked: (AddReplaceKeyOnChainData) -> Unit
) {
    val replacedSigners = data.stepDataMap.values.mapNotNull { it.signer }
    val displaySigners = replacedSigners.ifEmpty { data.originalSigners }
    val isReplaced = replacedSigners.isNotEmpty()
    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (banner, content, footer) = createRefs()
        val isVerified =
            data.verifyType != VerifyType.NONE ||
                    data.isInheritanceKey()
                        .not() && data.replaceSigners?.firstOrNull()?.type != SignerType.NFC
        Box(
            modifier = modifier
                .constrainAs(content) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
            if (displaySigners.isNotEmpty()) {
                val shouldShowDashLine =
                    displaySigners.size == 1 && isReplaced && data.isServerOrTimelockKey().not()
                val modifier = if (isReplaced.not()) {
                    Modifier.border(
                        BorderStroke(1.dp, colorResource(id = R.color.nc_stroke_primary)),
                        RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
                NcDashLineBox(
                    modifier = modifier,
                    showDashedBorder = shouldShowDashLine
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when {
                                    isReplaced.not() -> colorResource(id = R.color.nc_background_primary)
                                    displaySigners.size == 1 -> colorResource(id = R.color.nc_fill_slime).copy(
                                        alpha = 0.4f
                                    )

                                    isVerified -> colorResource(id = R.color.nc_fill_slime)
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
                                resId = displaySigners.firstOrNull()?.toReadableDrawableResId()
                                    ?: R.drawable.ic_hardware_key,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = displaySigners.firstOrNull()?.name ?: "Unknown Signer",
                                    style = NunchukTheme.typography.body
                                )

                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    NcTag(
                                        label = displaySigners.firstOrNull()
                                            ?.toReadableSignerType(context = LocalContext.current)
                                            ?: "Unknown",
                                        backgroundColor = colorResource(
                                            id = R.color.nc_bg_mid_gray
                                        ),
                                    )
                                    val firstAcctLabel =
                                        displaySigners.firstOrNull()?.let { firstSigner ->
                                            if (firstSigner.isShowAcctX(true)) {
                                                stringResource(
                                                    R.string.nc_acct_x,
                                                    if (firstSigner.index >= 0) firstSigner.index else 0
                                                )
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
                                        displaySigners.getOrNull(1)?.let { secondSigner ->
                                            if (secondSigner.isShowAcctX(true)) {
                                                stringResource(
                                                    R.string.nc_acct_x,
                                                    if (secondSigner.index >= 0) secondSigner.index else 0
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
                                    text = displaySigners.firstOrNull()?.getXfpOrCardIdLabel()
                                        ?: "",
                                    style = NunchukTheme.typography.bodySmall
                                )
                            }
                            if (displaySigners.size >= 2) {
                                if (isVerified || isReplaced.not()) {
                                    NcOutlineButton(
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = contentPadding,
                                        onClick = {
                                            if (isReplaced) onRemoveClicked(data) else onReplaceClicked(
                                                data
                                            )
                                        },
                                    ) {
                                        Text(
                                            text = if (isReplaced) "Remove" else "Replace",
                                            style = NunchukTheme.typography.captionTitle,
                                        )
                                    }
                                } else if (displaySigners.any { it.isVisible }) {
                                    NcOutlineButton(
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = contentPadding,
                                        onClick = { onVerifyClicked(data) },
                                    ) {
                                        Text(
                                            text = stringResource(R.string.nc_verify),
                                            style = NunchukTheme.typography.captionTitle
                                        )
                                    }
                                }
                            } else {
                                NcOutlineButton(
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = contentPadding,
                                    onClick = {
                                        onReplaceClicked(data)
                                    },
                                ) {
                                    Text(
                                        text = "Replace",
                                        style = NunchukTheme.typography.captionTitle,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (data.type.isAddInheritanceKey) {
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

        if (replacedSigners.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .constrainAs(footer) {
                        top.linkTo(content.bottom)
                        start.linkTo(content.start)
                    }
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_replace),
                    contentDescription = "Replace icon"
                )

                val originalSigner = data.originalSigners.firstOrNull()
                if (originalSigner != null) {
                    Text(
                        text = "Replacing ${originalSigner.name} (${originalSigner.getXfpOrCardIdLabel()})",
                        style = NunchukTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplaceServerCard(
    item: AddReplaceKeyOnChainData,
) {
    Row(
        modifier = Modifier
            .border(
                BorderStroke(1.dp, colorResource(id = R.color.nc_stroke_primary)),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcCircleImage(resId = item.type.resId)
        Column(
            modifier = Modifier
                .weight(1.0f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = "Platform Key",
                style = NunchukTheme.typography.body
            )
        }
    }
}

@Composable
private fun TimelockReplaceCard(
    modifier: Modifier = Modifier,
    data: AddReplaceKeyOnChainData,
    onChangeClicked: () -> Unit = {},
) {
    val isTimelockWithData = (data.newTimelock ?: data.originalTimelock) != null
    val isTimelockReplaced = data.newTimelock != null
    val displayedTimelock = data.newTimelock ?: data.originalTimelock
    val formattedDate = if (isTimelockWithData) {
        val dateFormat =
            SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone(displayedTimelock?.timezone)
        dateFormat.format(Date(displayedTimelock?.timelockValue!! * 1000))
    } else ""
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = modifier
                .background(
                    color = if (isTimelockReplaced) colorResource(id = R.color.nc_fill_slime) else colorResource(
                        id = R.color.nc_background_primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    BorderStroke(1.dp, colorResource(id = R.color.nc_stroke_primary)),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NcCircleImage(resId = R.drawable.ic_timer)
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = if (isTimelockWithData) "After" else stringResource(R.string.nc_timelock),
                        style = NunchukTheme.typography.body
                    )
                    if (isTimelockWithData) {
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = formattedDate,
                            style = NunchukTheme.typography.body
                        )
                    }
                }
                NcOutlineButton(
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    onClick = onChangeClicked,
                ) {
                    Text(
                        text = "Change",
                        style = NunchukTheme.typography.captionTitle,
                    )
                }
            }
        }
        if (isTimelockReplaced) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_replace),
                    contentDescription = "Replace icon"
                )

                val formattedDate = data.originalTimelock?.let {
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    it.timezone.let { timezoneId ->
                        dateFormat.timeZone = TimeZone.getTimeZone(timezoneId)
                    }
                    dateFormat.format(Date(it.timelockValue * 1000))
                } ?: ""
                Text(
                    text = if (formattedDate.isNotEmpty()) {
                        "Replacing existing timelock ($formattedDate)"
                    } else {
                        "Replacing existing timelock"
                    },
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun OnChainReplaceKeysContentPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    OnChainReplaceKeysContent(
        uiState = OnChainReplaceKeysUiState(
            walletSigners = signers
        ),
        keys = listOf(
            AddReplaceKeyOnChainData(
                steps = listOf(OnChainReplaceKeyStep.INHERITANCE_KEY),
                stepDataMap = mapOf(
                    OnChainReplaceKeyStep.HARDWARE_KEY to ReplaceStepData(
                        signer = signers[0],
                        verifyType = VerifyType.NONE
                    )
                )
            ),
        ),
    )
}