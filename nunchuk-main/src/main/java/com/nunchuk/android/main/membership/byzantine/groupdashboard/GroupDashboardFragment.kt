package com.nunchuk.android.main.membership.byzantine.groupdashboard

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.domain.membership.PasswordVerificationHelper
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.groupchathistory.GroupChatHistoryArgs
import com.nunchuk.android.core.groupchathistory.GroupChatHistoryFragment
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.keyrecovery.KeyRecoverySuccessState
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.main.membership.byzantine.groupdashboard.action.AlertActionIntroFragment
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentActivity
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.AlertType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.byzantine.isInheritanceFlow
import com.nunchuk.android.model.byzantine.isInheritanceType
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.isMasterOrAdminOrFacilitatorAdmin
import com.nunchuk.android.nav.args.MembershipArgs
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.network.IsNetworkConnectedUseCase
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GroupDashboardFragment : BaseFragment<ViewBinding>(), BottomSheetOptionListener {

    @Inject
    lateinit var isNetworkConnectedUseCase: IsNetworkConnectedUseCase

    @Inject
    lateinit var passwordVerificationHelper: PasswordVerificationHelper

    private val args: GroupDashboardFragmentArgs by navArgs()

    private val viewModel: GroupDashboardViewModel by activityViewModels()

    private val createWalletLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                requireActivity().finish()
            }
        }

    private val registerWalletLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                findNavController().navigate(
                    GroupDashboardFragmentDirections.actionGroupDashboardFragmentToWalletConfigIntroFragment()
                )
                viewModel.dismissCurrentAlert()
            }
        }

    private val inheritanceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (it.data?.getBooleanExtra(
                        InheritancePlanningActivity.RESULT_REQUEST_PLANNING,
                        false
                    ) == true
                ) {
                    viewModel.setInheritanceRequestByMe()
                } else {
                    val dummyTransactionId =
                        it.data?.getStringExtra(GlobalResultKey.DUMMY_TX_ID).orEmpty()
                    val requiredSignatures =
                        it.data?.getIntExtra(GlobalResultKey.REQUIRED_SIGNATURES, 0) ?: 0
                    if (dummyTransactionId.isNotEmpty()) {
                        openWalletAuthentication(
                            dummyTransactionId = dummyTransactionId,
                            requiredSignatures = requiredSignatures,
                        )
                    }
                }
            }
        }

    private val signLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val dummyTransactionType =
                    data.parcelable<DummyTransactionType>(GlobalResultKey.EXTRA_DUMMY_TX_TYPE)
                if (dummyTransactionType != null) {
                    if (dummyTransactionType.isInheritanceFlow()) {
                        viewModel.markSetupInheritance(dummyTransactionType)
                        showInheritanceMessage(dummyTransactionType)
                    } else if (dummyTransactionType == DummyTransactionType.KEY_RECOVERY_REQUEST) {
                        findNavController().navigate(
                            GroupDashboardFragmentDirections.actionGroupDashboardFragmentToKeyRecoverySuccessStateFragment(
                                type = KeyRecoverySuccessState.KEY_RECOVERY_APPROVED.name
                            )
                        )
                    } else if (dummyTransactionType == DummyTransactionType.CHANGE_EMAIL) {
                        viewModel.handleSignOutEvent()
                    }
                }
            }
        }

    private fun showInheritanceMessage(dummyTransactionType: DummyTransactionType) {
        val message = when (dummyTransactionType) {
            DummyTransactionType.CREATE_INHERITANCE_PLAN -> getString(R.string.nc_inheritance_has_been_created)
            DummyTransactionType.UPDATE_INHERITANCE_PLAN -> {
                if (args.groupId?.isNotEmpty() == true) {
                    getString(R.string.nc_inheritance_has_been_updated)
                } else {
                    getString(R.string.nc_inheritance_plan_updated)
                }
            }

            DummyTransactionType.CANCEL_INHERITANCE_PLAN -> getString(R.string.nc_inheritance_has_been_canlled)
            else -> ""
        }
        if (message.isNotEmpty()) {
            showSuccess(message = message)
        }
    }

    private val isPendingPersonalWallet by lazy { args.walletId.isNullOrEmpty() && args.groupId.isNullOrEmpty() }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding =
        ViewBinding { View(context) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                GroupDashboardScreen(
                    viewModel = viewModel,
                    onEditClick = {
                        networkCheck {
                            findNavController().navigate(
                                GroupDashboardFragmentDirections.actionGroupDashboardFragmentToByzantineInviteMembersFragment(
                                    members = viewModel.getMembers().toTypedArray(),
                                    groupId = viewModel.getByzantineGroup()?.id.orEmpty(),
                                    flow = ByzantineMemberFlow.EDIT,
                                    groupType = viewModel.getByzantineGroup()?.walletConfig?.toGroupWalletType()?.name.orEmpty(),
                                    walletId = viewModel.getWalletId(),
                                    groupRole = viewModel.getMyRole().name
                                )
                            )
                        }
                    },
                    onAlertClick = { alert, role ->
                        alertClick(alert, role)
                    },
                    onWalletClick = {
                        args.walletId?.let {
                            openWalletDetailsScreen(it)
                        }
                    },
                    onGroupChatClick = {
                        networkCheck {
                            if (viewModel.groupChat() != null) {
                                openRoomChat()
                            } else {
                                viewModel.getByzantineGroup()?.let { group ->
                                    findNavController().navigate(
                                        GroupDashboardFragmentDirections.actionGroupDashboardFragmentToGroupChatHistoryIntroFragment(
                                            group,
                                            viewModel.state.value.wallet.name
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onMoreClick = {
                        if (args.groupId.isNullOrEmpty()) {
                            showMoreOptionsNormalAssistedWallet()
                        } else {
                            showMoreOptionsByzantine()
                        }
                    },
                    onOpenHealthCheckScreen = {
                        openHealthCheckScreen()
                    },
                )
            }
        }
    }

    private fun openHealthCheckScreen() {
        val walletId = viewModel.getWalletId()
        if (walletId.isNotEmpty()) {
            findNavController().navigate(
                GroupDashboardFragmentDirections.actionGroupDashboardFragmentToHealthCheckFragment(
                    groupId = viewModel.getGroupId(),
                    walletId = viewModel.getWalletId()
                )
            )
        }
    }

    private fun openWalletAuthentication(
        dummyTransactionId: String,
        requiredSignatures: Int,
        userData: String = "",
    ) {
        navigator.openWalletAuthentication(
            activityContext = requireActivity(),
            walletId = viewModel.getWalletId(),
            requiredSignatures = requiredSignatures,
            type = VerificationType.SIGN_DUMMY_TX,
            groupId = viewModel.getGroupId(),
            dummyTransactionId = dummyTransactionId,
            userData = userData,
            launcher = signLauncher
        )
    }

    private fun networkCheck(block: () -> Unit) {
        if (isNetworkConnectedUseCase().not()) {
            showError(message = getString(R.string.nc_no_internet_connection_try_again_later))
        } else {
            block()
        }
    }

    private fun openWalletDetailsScreen(walletId: String) {
        navigator.openWalletDetailsScreen(
            activityContext = requireActivity(),
            walletId = walletId
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().supportFragmentManager.setFragmentResultListener(
            GroupChatHistoryFragment.REQUEST_KEY, this
        ) { _: String?, bundle: Bundle ->
            val historyPeriod =
                bundle.parcelable<HistoryPeriod>(GroupChatHistoryFragment.EXTRA_HISTORY_PERIOD)
            viewModel.updateGroupChatHistoryPeriod(historyPeriod)
            showSuccess(message = getString(R.string.nc_chat_setting_updated))
            clearFragmentResult(GroupChatHistoryFragment.REQUEST_KEY)
        }
        setFragmentResultListener(GroupChatHistoryIntroFragment.REQUEST_KEY) { _, bundle ->
            val groupChat =
                bundle.parcelable<GroupChat>(GroupChatHistoryIntroFragment.EXTRA_GROUP_CHAT)
                    ?: return@setFragmentResultListener
            viewModel.updateGroupChat(groupChat)
            clearFragmentResult(GroupChatHistoryIntroFragment.REQUEST_KEY)
        }
        setFragmentResultListener(AlertActionIntroFragment.REQUEST_KEY) { _, bundle ->
            if (bundle.containsKey(AlertActionIntroFragment.EXTRA_APPROVE_INHERITANCE_REQUEST)) {
                if (bundle.getBoolean(AlertActionIntroFragment.EXTRA_APPROVE_INHERITANCE_REQUEST)) {
                    showSuccess(message = getString(R.string.nc_inheritance_request_approved))
                } else {
                    showSuccess(message = getString(R.string.nc_inheritance_request_denied))
                }
                viewModel.getInheritance(silentLoading = true)
            } else {
                val dummyTransactionId =
                    bundle.getString(AlertActionIntroFragment.EXTRA_DUMMY_TRANSACTION_ID).orEmpty()
                val requiredSignatures = bundle.getInt(AlertActionIntroFragment.EXTRA_REQUIRE_KEY)
                if (dummyTransactionId.isNotEmpty()) {
                    openWalletAuthentication(
                        dummyTransactionId = dummyTransactionId,
                        requiredSignatures = requiredSignatures,
                    )
                }
            }
            clearFragmentResult(AlertActionIntroFragment.REQUEST_KEY)
        }

        flowObserver(viewModel.event) { event ->
            when (event) {
                is GroupDashboardEvent.Error -> showError(message = event.message)
                is GroupDashboardEvent.Loading -> showOrHideLoading(event.loading)
                is GroupDashboardEvent.GetHistoryPeriodSuccess -> {
                    viewModel.groupChat()?.roomId?.let { roomId ->
                        GroupChatHistoryFragment.show(
                            childFragmentManager,
                            GroupChatHistoryArgs(
                                historyPeriods = event.periods,
                                historyPeriodIdSelected = viewModel.groupChat()?.historyPeriod?.id.orEmpty(),
                                isFreeGroupWalletFlow = false,
                                roomId = roomId,
                                groupId = viewModel.getGroupId(),
                                walletId = viewModel.getWalletId()
                            )
                        )
                    }
                }

                is GroupDashboardEvent.GetHealthCheckPayload -> {}
                GroupDashboardEvent.RequestHealthCheckSuccess -> {}
                is GroupDashboardEvent.GetInheritanceSuccess -> {
                    if (event.isAlertFlow) {
                        viewModel.dismissCurrentAlert()
                        findNavController().navigate(
                            GroupDashboardFragmentDirections.actionGroupDashboardFragmentToInheritanceCreateSuccessFragment(
                                magicalPhrase = event.inheritance.magic,
                                planFlow = InheritancePlanFlow.VIEW,
                                walletId = args.walletId.orEmpty(),
                                sourceFlow = InheritanceSourceFlow.GROUP_DASHBOARD,
                            )
                        )
                    } else {
                        if (event.token.isNotEmpty()) {
                            navigator.openInheritancePlanningScreen(
                                walletId = viewModel.getWalletId(),
                                activityContext = requireContext(),
                                verifyToken = event.token,
                                inheritance = event.inheritance,
                                flowInfo = InheritancePlanFlow.VIEW,
                                groupId = viewModel.getGroupId(),
                                sourceFlow = InheritanceSourceFlow.GROUP_DASHBOARD,
                            )
                        } else if (event.inheritance.status == InheritanceStatus.PENDING_APPROVAL) {
                            viewModel.calculateRequiredSignatures()
                        } else {
                            navigator.openInheritancePlanningScreen(
                                walletId = viewModel.getWalletId(),
                                activityContext = requireContext(),
                                flowInfo = InheritancePlanFlow.SETUP,
                                groupId = viewModel.getGroupId(),
                                sourceFlow = InheritanceSourceFlow.GROUP_DASHBOARD,
                            )
                        }
                    }
                }

                is GroupDashboardEvent.RegisterSignersSuccess -> {
                    registerWalletLauncher.launch(
                        MembershipActivity.openRegisterWalletIntent(
                            activity = requireActivity(),
                            args = MembershipArgs(
                                groupStep = MembershipStage.REGISTER_WALLET,
                                groupId = viewModel.getGroupId(),
                                walletId = viewModel.getWalletId()
                            )
                        )
                    )
                }



                is GroupDashboardEvent.CalculateRequiredSignaturesSuccess -> {
                    if (event.type == "NONE") {
                        navigator.openInheritancePlanningScreen(
                            walletId = viewModel.getWalletId(),
                            activityContext = requireContext(),
                            flowInfo = InheritancePlanFlow.REQUEST,
                            groupId = viewModel.getGroupId(),
                            sourceFlow = InheritanceSourceFlow.GROUP_DASHBOARD,
                            launcher = inheritanceLauncher
                        )
                    }
                }

                GroupDashboardEvent.RestartWizardSuccess -> requireActivity().finish()
                is GroupDashboardEvent.DownloadBackupKeySuccess -> {
                    findNavController().navigate(
                        GroupDashboardFragmentDirections.actionGroupDashboardFragmentToBackupDownloadFragment(
                            backupKey = event.backupKey
                        )
                    )
                }

                is GroupDashboardEvent.SyncTransactionSuccess -> {
                    navigator.openTransactionDetailsScreen(
                        activityContext = requireActivity(),
                        walletId = viewModel.getWalletId(),
                        txId = event.txId,
                        isRequestSignatureFlow = true,
                    )
                }

                GroupDashboardEvent.SignOutEvent -> {
                    hideLoading()
                    NcToastManager.scheduleShowMessage(
                        message = getString(com.nunchuk.android.settings.R.string.nc_email_has_been_changed),
                    )
                    navigator.restartApp(requireActivity())
                }


            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAlerts()
        viewModel.getKeysStatus()
    }

    private fun enterPasswordDialog(targetAction: TargetAction) {
        passwordVerificationHelper.showPasswordVerificationDialog(
            context = requireContext(),
            targetAction = targetAction,
            coroutineScope = lifecycleScope,
            onSuccess = { token ->
                when (targetAction) {
                    TargetAction.UPDATE_INHERITANCE_PLAN -> viewModel.getInheritance(token)
                    TargetAction.UPDATE_SERVER_KEY -> {
                        viewModel.state.value.signers.find { it.type == SignerType.SERVER }?.let { signer ->
                            CosigningPolicyActivity.start(
                                activity = requireActivity(),
                                signer = signer,
                                token = token,
                                walletId = viewModel.getWalletId(),
                                groupId = viewModel.getGroupId(),
                            )
                        }
                    }
                    TargetAction.EMERGENCY_LOCKDOWN -> {
                        navigator.openEmergencyLockdownScreen(
                            activityContext = requireActivity(),
                            verifyToken = token,
                            walletId = viewModel.getWalletId(),
                            groupId = viewModel.getGroupId(),
                        )
                    }
                    TargetAction.REPLACE_KEYS -> {
                        navigator.openMembershipActivity(
                            activityContext = requireActivity(),
                            groupStep = MembershipStage.REPLACE_KEY,
                            walletId = viewModel.getWalletId(),
                            groupId = viewModel.getGroupId(),
                        )
                    }
                    else -> {}
                }
            },
            onError = { errorMessage ->
                showError(errorMessage)
            }
        )
    }

    private fun alertClick(alert: Alert, role: AssistedWalletRole) {
        viewModel.setCurrentSelectedAlert(alert)
        if (alert.type == AlertType.WALLET_PENDING) {
            val walletType = viewModel.state.value.personalWalletType
            when(walletType) {
                GroupWalletType.TWO_OF_THREE_PLATFORM_KEY -> viewModel.setLocalMembershipPlan(MembershipPlan.IRON_HAND)
                GroupWalletType.TWO_OF_FOUR_MULTISIG -> viewModel.setLocalMembershipPlan(MembershipPlan.HONEY_BADGER)
                else -> Unit
            }
            navigator.openMembershipActivity(
                launcher = createWalletLauncher,
                activityContext = requireActivity(),
                groupStep = MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS,
                isPersonalWallet = true,
                walletType = viewModel.state.value.personalWalletType,
            )
        } else if (alert.type == AlertType.GROUP_WALLET_PENDING) {
            val walletType = viewModel.getByzantineGroup()?.walletConfig?.toGroupWalletType()
            if (role.isMasterOrAdminOrFacilitatorAdmin) {
                navigator.openMembershipActivity(
                    launcher = createWalletLauncher,
                    activityContext = requireActivity(),
                    groupStep = MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS,
                    walletId = args.walletId,
                    groupId = viewModel.getGroupId(),
                    isPersonalWallet = true,
                    walletType = walletType
                )
            } else {
                navigator.openMembershipActivity(
                    launcher = createWalletLauncher,
                    activityContext = requireActivity(),
                    groupStep = MembershipStage.ADD_KEY_ONLY,
                    groupId = viewModel.getGroupId(),
                    isPersonalWallet = true,
                    walletType = walletType
                )
            }
        } else if (alert.type == AlertType.UPDATE_SERVER_KEY) {
            val dummyTransactionId = alert.payload.dummyTransactionId
            if (dummyTransactionId.isNotEmpty()) {
                CosigningPolicyActivity.start(
                    activity = requireActivity(),
                    walletId = viewModel.getWalletId(),
                    groupId = viewModel.getGroupId(),
                    dummyTransactionId = alert.payload.dummyTransactionId,
                )
            }
        } else if (alert.type.isInheritanceType()) {
            navigator.openInheritancePlanningScreen(
                launcher = inheritanceLauncher,
                walletId = viewModel.getWalletId(),
                activityContext = requireActivity(),
                flowInfo = InheritancePlanFlow.SIGN_DUMMY_TX,
                sourceFlow = InheritanceSourceFlow.GROUP_DASHBOARD,
                groupId = viewModel.getGroupId(),
                dummyTransactionId = alert.payload.dummyTransactionId
            )
        } else if (alert.type == AlertType.CREATE_INHERITANCE_PLAN_SUCCESS) {
            viewModel.getInheritance(isAlertFlow = true)
        } else if (alert.type == AlertType.GROUP_WALLET_SETUP) {
            if (alert.payload.claimKey) {
                findNavController().navigate(
                    GroupDashboardFragmentDirections.actionGroupDashboardFragmentToClaimKeyFragment(
                        groupId = viewModel.getGroupId(),
                        walletId = viewModel.getWalletId(),
                        myRole = viewModel.state.value.myRole,
                    )
                )
            } else {
                viewModel.handleRegisterSigners(alert.id, alert.payload.xfps)
            }
        } else if (alert.type == AlertType.REQUEST_INHERITANCE_PLANNING_APPROVED) {
            navigator.openInheritancePlanningScreen(
                walletId = viewModel.getWalletId(),
                activityContext = requireContext(),
                flowInfo = InheritancePlanFlow.SETUP,
                groupId = viewModel.getGroupId(),
                sourceFlow = InheritanceSourceFlow.GROUP_DASHBOARD,
            )
        } else if (alert.type == AlertType.KEY_RECOVERY_REQUEST
            || alert.type == AlertType.RECURRING_PAYMENT_CANCELATION_PENDING
            || alert.type == AlertType.UPDATE_SECURITY_QUESTIONS
            || alert.type == AlertType.REQUEST_INHERITANCE_PLANNING
            || alert.type == AlertType.HEALTH_CHECK_REQUEST
            || alert.type == AlertType.HEALTH_CHECK_PENDING
            || alert.type == AlertType.CHANGE_EMAIL_REQUEST
            || alert.type == AlertType.HEALTH_CHECK_REMINDER
            || alert.type == AlertType.KEY_REPLACEMENT_PENDING
        ) {
            findNavController().navigate(
                GroupDashboardFragmentDirections.actionGroupDashboardFragmentToAlertActionIntroFragment(
                    viewModel.getGroupId(),
                    viewModel.getWalletId(),
                    alert
                )
            )
        } else if (alert.type == AlertType.KEY_RECOVERY_APPROVED) {
            viewModel.recoverKey(alert.payload.keyXfp)
        } else if (alert.type == AlertType.RECURRING_PAYMENT_REQUEST) {
            findNavController().navigate(
                GroupDashboardFragmentDirections.actionGroupDashboardFragmentToRecurringPaymentRequestFragment(
                    viewModel.getGroupId(),
                    viewModel.getWalletId(),
                    alert.payload.dummyTransactionId
                )
            )
        } else if (alert.type == AlertType.TRANSACTION_SIGNATURE_REQUEST) {
            viewModel.syncTransaction(alert.payload.transactionId)
        } else if (alert.type == AlertType.KEY_REPLACEMENT_COMPLETED) {
            enterPasswordDialog(TargetAction.REPLACE_KEYS)
        } else if (alert.type == AlertType.SETUP_INHERITANCE_PLAN) {
            viewModel.getInheritance()
        } else if (alert.type == AlertType.TRANSFER_FUNDS) {
            navigator.openRollOverWalletScreen(
                activityContext = requireActivity(),
                oldWalletId = viewModel.getWalletId(),
                newWalletId = alert.payload.newWalletId,
                startScreen = RollOverWalletFlow.REFUND,
                source = RollOverWalletSource.REPLACE_KEY
            )
        }
    }

    private fun openRoomChat() {
        navigator.openRoomDetailActivity(
            activityContext = requireActivity(),
            roomId = viewModel.groupChat()!!.roomId,
            isGroupChat = true
        )
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.SET_UP_INHERITANCE -> {
                if (viewModel.state.value.isAlreadySetupInheritance) {
                    enterPasswordDialog(TargetAction.UPDATE_INHERITANCE_PLAN)
                } else {
                    viewModel.getInheritance()
                }
            }

            SheetOptionType.TYPE_PLATFORM_KEY_POLICY -> {
                enterPasswordDialog(TargetAction.UPDATE_SERVER_KEY)
            }

            SheetOptionType.TYPE_EMERGENCY_LOCKDOWN -> {
                enterPasswordDialog(TargetAction.EMERGENCY_LOCKDOWN)
            }

            SheetOptionType.TYPE_RECURRING_PAYMENT -> {
                RecurringPaymentActivity.navigate(
                    activity = requireActivity(),
                    groupId = viewModel.getGroupId(),
                    walletId = viewModel.getWalletId(),
                    role = viewModel.state.value.myRole,
                )
            }

            SheetOptionType.TYPE_GROUP_CHAT_HISTORY -> {
                viewModel.getGroupChatHistoryPeriod()
            }

            SheetOptionType.TYPE_RESTART_WIZARD -> {
                NCWarningDialog(requireActivity()).showDialog(
                    title = getString(R.string.nc_confirmation),
                    message = getString(R.string.nc_confirm_cancel_pending_wallet),
                    onYesClick = {
                        viewModel.restartWizard()
                    }
                )
            }

            SheetOptionType.TYPE_REPLACE_KEY -> {
                enterPasswordDialog(TargetAction.REPLACE_KEYS)
            }
        }
    }

    private fun showMoreOptionsByzantine() {
        val options = mutableListOf<SheetOption>()
        val uiState = viewModel.state.value
        if (viewModel.isPendingCreateWallet().not()) {
            if (uiState.group?.walletConfig?.allowInheritance == true) {
                if (uiState.myRole.isMasterOrAdmin) {
                    if (uiState.isAlreadySetupInheritance) {
                        options.add(
                            SheetOption(
                                type = SheetOptionType.SET_UP_INHERITANCE,
                                stringId = R.string.nc_view_inheritance_plan
                            ),
                        )
                    } else if (viewModel.isShowSetupInheritanceOption()) {
                        options.add(
                            SheetOption(
                                type = SheetOptionType.SET_UP_INHERITANCE,
                                stringId = R.string.nc_set_up_inheritance_plan_wallet
                            ),
                        )
                    }
                } else if (uiState.isAlreadySetupInheritance) {
                    options.add(
                        SheetOption(
                            type = SheetOptionType.SET_UP_INHERITANCE,
                            stringId = R.string.nc_view_inheritance_plan
                        ),
                    )
                }
            }
            if (uiState.group?.walletConfig?.requiredServerKey == true) {
                if (!args.walletId.isNullOrEmpty()) {
                    options.add(
                        SheetOption(
                            type = SheetOptionType.TYPE_PLATFORM_KEY_POLICY,
                            stringId = R.string.nc_cosigning_policies
                        )
                    )
                }
            }
            if (uiState.myRole.isMasterOrAdmin) {
                options.add(
                    SheetOption(
                        type = SheetOptionType.TYPE_EMERGENCY_LOCKDOWN,
                        stringId = R.string.nc_emergency_lockdown
                    )
                )
            }
            if (uiState.myRole.isMasterOrAdmin) {
                options.add(
                    SheetOption(
                        type = SheetOptionType.TYPE_REPLACE_KEY,
                        stringId = R.string.nc_replace_keys
                    )
                )
            }
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_RECURRING_PAYMENT,
                    stringId = R.string.nc_view_recurring_payments
                )
            )
        }
        if (uiState.myRole.isMasterOrAdmin && viewModel.groupChat() != null) {
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_GROUP_CHAT_HISTORY,
                    stringId = R.string.nc_manage_group_chat_history
                )
            )
        }
        if (viewModel.isPendingCreateWallet() && uiState.myRole == AssistedWalletRole.MASTER) {
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_RESTART_WIZARD,
                    stringId = R.string.nc_cancel_pending_wallet,
                    isDeleted = true
                )
            )
        }
        if (options.isEmpty()) return
        val bottomSheet = BottomSheetOption.newInstance(options)
        bottomSheet.show(childFragmentManager, "BottomSheetOption")
    }

    private fun showMoreOptionsNormalAssistedWallet() {
        val options = mutableListOf<SheetOption>()
        val uiState = viewModel.state.value
        if (viewModel.isPendingCreateWallet().not() && !isPendingPersonalWallet) {
            if (viewModel.membershipPlan() != MembershipPlan.IRON_HAND) {
                if (uiState.isAlreadySetupInheritance) {
                    options.add(
                        SheetOption(
                            type = SheetOptionType.SET_UP_INHERITANCE,
                            stringId = R.string.nc_view_inheritance_plan
                        ),
                    )
                } else if (viewModel.isShowSetupInheritanceOption()) {
                    options.add(
                        SheetOption(
                            type = SheetOptionType.SET_UP_INHERITANCE,
                            stringId = R.string.nc_set_up_inheritance_plan_wallet
                        ),
                    )
                }
            }
            if (!args.walletId.isNullOrEmpty()) {
                options.add(
                    SheetOption(
                        type = SheetOptionType.TYPE_PLATFORM_KEY_POLICY,
                        stringId = R.string.nc_cosigning_policies
                    )
                )
            }
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_EMERGENCY_LOCKDOWN,
                    stringId = R.string.nc_emergency_lockdown
                )
            )
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_REPLACE_KEY,
                    stringId = R.string.nc_replace_keys
                )
            )
        }
        if (viewModel.isPendingCreateWallet() || isPendingPersonalWallet) {
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_RESTART_WIZARD,
                    stringId = R.string.nc_cancel_pending_wallet,
                    isDeleted = true
                )
            )
        }
        if (options.isEmpty()) return
        val bottomSheet = BottomSheetOption.newInstance(options)
        bottomSheet.show(childFragmentManager, "BottomSheetOption")
    }
}

@Composable
private fun GroupDashboardScreen(
    viewModel: GroupDashboardViewModel = viewModel(),
    onEditClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onAlertClick: (alert: Alert, role: AssistedWalletRole) -> Unit = { _, _ -> },
    onGroupChatClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onOpenHealthCheckScreen: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupDashboardContent(
        uiState = state,
        onDismissClick = viewModel::dismissAlert,
        isEnableStartGroupChat = viewModel.isEnableStartGroupChat(),
        refresh = viewModel::refresh,
        isRefreshing = state.isRefreshing,
        onEditClick = onEditClick,
        onWalletClick = onWalletClick,
        onAlertClick = { alert, role ->
            onAlertClick(alert, role)
            viewModel.markAsReadAlert(alert.id)
        },
        onGroupChatClick = onGroupChatClick,
        onMoreClick = onMoreClick,
        onOpenHealthCheckScreen = onOpenHealthCheckScreen
    )
}

@Preview
@Composable
private fun GroupDashboardScreenPreview() {
    GroupDashboardContent()
}