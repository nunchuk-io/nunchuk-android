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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.main.membership.byzantine.groupchathistory.GroupChatHistoryFragment
import com.nunchuk.android.main.membership.byzantine.groupdashboard.action.AlertActionIntroFragment
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.AlertType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.isInheritanceType
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.nunchuk.android.widget.NCInputDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GroupDashboardFragment : MembershipFragment(), BottomSheetOptionListener {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: GroupDashboardFragmentArgs by navArgs()

    private val viewModel: GroupDashboardViewModel by activityViewModels()

    private val createWalletLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                requireActivity().finish()
            }
        }

    private val healthCheckLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val type =
                    it.data?.serializable<DummyTransactionType>(GlobalResultKey.EXTRA_DUMMY_TX_TYPE)
                if (type == DummyTransactionType.HEALTH_CHECK_PENDING || type == DummyTransactionType.HEALTH_CHECK_REQUEST) {
                    val xfp =
                        it.data?.getStringExtra(GlobalResultKey.EXTRA_HEALTH_CHECK_XFP).orEmpty()
                    viewModel.getSignerName(xfp)?.let { name ->
                        showSuccess(
                            message = getString(
                                R.string.nc_txt_run_health_check_success_event,
                                name
                            )
                        )
                    }
                }
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                GroupDashboardScreen(
                    viewModel,
                    onEditClick = {
                        findNavController().navigate(
                            GroupDashboardFragmentDirections.actionGroupDashboardFragmentToByzantineInviteMembersFragment(
                                members = viewModel.getMembers().toTypedArray(),
                                groupId = viewModel.getByzantineGroup()?.id.orEmpty(),
                                flow = ByzantineMemberFlow.EDIT,
                                groupType = viewModel.getByzantineGroup()?.walletConfig?.toGroupWalletType()?.name.orEmpty(),
                            )
                        )
                    },
                    onAlertClick = { alert, role ->
                        alertClick(alert, role)
                    },
                    onWalletClick = {
                        args.walletId?.let {
                            navigator.openWalletDetailsScreen(
                                activityContext = requireActivity(),
                                walletId = it
                            )
                        }
                    },
                    onGroupChatClick = {
                        if (viewModel.groupChat() != null) {
                            openRoomChat()
                        } else {
                            findNavController().navigate(
                                GroupDashboardFragmentDirections.actionGroupDashboardFragmentToGroupChatHistoryIntroFragment(
                                    args.groupId
                                )
                            )
                        }
                    },
                    onMoreClick = {
                        showMoreOptions()
                    },
                    onOpenHealthCheckScreen = {
                        val walletId = viewModel.getWalletId()
                        if (walletId.isNotEmpty()) {
                            findNavController().navigate(
                                GroupDashboardFragmentDirections.actionGroupDashboardFragmentToHealthCheckFragment(
                                    groupId = args.groupId,
                                    walletId = viewModel.getWalletId()
                                )
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(GroupChatHistoryFragment.REQUEST_KEY) { _, bundle ->
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
            val dummyTransactionId =
                bundle.getString(AlertActionIntroFragment.EXTRA_DUMMY_TRANSACTION_ID).orEmpty()
            val requiredSignatures = bundle.getInt(AlertActionIntroFragment.EXTRA_REQUIRE_KEY)
            if (dummyTransactionId.isNotEmpty()) {
                navigator.openWalletAuthentication(
                    activityContext = requireActivity(),
                    walletId = viewModel.getWalletId(),
                    requiredSignatures = requiredSignatures,
                    type = VerificationType.SIGN_DUMMY_TX,
                    groupId = args.groupId,
                    dummyTransactionId = dummyTransactionId,
                    launcher = healthCheckLauncher
                )
            }
            clearFragmentResult(AlertActionIntroFragment.REQUEST_KEY)
        }
        flowObserver(viewModel.event) { event ->
            when (event) {
                is GroupDashboardEvent.Error -> showError(message = event.message)
                is GroupDashboardEvent.Loading -> showOrHideLoading(event.loading)
                is GroupDashboardEvent.GetHistoryPeriodSuccess -> {
                    findNavController().navigate(
                        GroupDashboardFragmentDirections.actionGroupDashboardFragmentToGroupChatHistoryFragment(
                            periods = event.periods.toTypedArray(),
                            groupId = viewModel.getByzantineGroup()?.id.orEmpty(),
                            historyPeriodId = viewModel.groupChat()?.historyPeriod?.id.orEmpty()
                        )
                    )
                }

                is GroupDashboardEvent.GetHealthCheckPayload -> {}
                GroupDashboardEvent.RequestHealthCheckSuccess -> {}
                is GroupDashboardEvent.GetInheritanceSuccess -> {
                    if (event.isOpenReviewInheritance) {
                        navigator.openInheritancePlanningScreen(
                            walletId = viewModel.getWalletId(),
                            requireContext(),
                            verifyToken = event.token,
                            inheritance = event.inheritance,
                            flowInfo = InheritancePlanFlow.VIEW,
                            groupId = args.groupId
                        )
                    } else {
                        findNavController().navigate(
                            GroupDashboardFragmentDirections.actionGroupDashboardFragmentToInheritanceCreateSuccessFragment(
                                magicalPhrase = event.inheritance.magic,
                                planFlow = InheritancePlanFlow.VIEW,
                                walletId = args.walletId.orEmpty(),
                                isOpenFromWizard = false
                            )
                        )
                    }
                }

                is GroupDashboardEvent.RegisterSignersSuccess -> {
                    registerWalletLauncher.launch(
                        MembershipActivity.openRegisterWalletIntent(
                            activity = requireActivity(),
                            groupId = args.groupId,
                            walletId = viewModel.getWalletId(),
                            index = event.totalColdcard,
                            airgapIndex = event.totalAirgap
                        )
                    )
                }

                is GroupDashboardEvent.UpdateServerKey -> CosigningPolicyActivity.start(
                    activity = requireActivity(),
                    signer = event.signer,
                    token = event.token,
                    walletId = args.walletId.orEmpty(),
                    groupId = event.groupId,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAlerts()
        viewModel.getKeysStatus()
    }

    private fun enterPasswordDialog(targetAction: TargetAction) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                viewModel.confirmPassword(it, targetAction)
            }
        )
    }

    private fun alertClick(alert: Alert, role: AssistedWalletRole) {
        viewModel.setCurrentSelectedAlert(alert)
        if (alert.type == AlertType.GROUP_WALLET_PENDING) {
            if (role.isMasterOrAdmin) {
                navigator.openMembershipActivity(
                    launcher = createWalletLauncher,
                    activityContext = requireActivity(),
                    groupStep = MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS,
                    groupId = args.groupId,
                    walletId = args.walletId
                )
            } else {
                navigator.openMembershipActivity(
                    launcher = createWalletLauncher,
                    activityContext = requireActivity(),
                    groupStep = MembershipStage.ADD_KEY_ONLY,
                    groupId = args.groupId
                )
            }
        } else if (alert.type == AlertType.UPDATE_SERVER_KEY) {
            val dummyTransactionId = alert.payload.dummyTransactionId
            if (dummyTransactionId.isNotEmpty()) {
                CosigningPolicyActivity.start(
                    activity = requireActivity(),
                    walletId = viewModel.getWalletId(),
                    groupId = args.groupId,
                    dummyTransactionId = alert.payload.dummyTransactionId,
                )
            }
        } else if (alert.type.isInheritanceType()) {
            navigator.openInheritancePlanningScreen(
                walletId = viewModel.getWalletId(),
                activityContext = requireActivity(),
                flowInfo = InheritancePlanFlow.SIGN_DUMMY_TX,
                isOpenFromWizard = false,
                groupId = args.groupId,
                dummyTransactionId = alert.payload.dummyTransactionId
            )
        } else if (alert.type == AlertType.HEALTH_CHECK_REQUEST || alert.type == AlertType.HEALTH_CHECK_PENDING) {
            findNavController().navigate(
                GroupDashboardFragmentDirections.actionGroupDashboardFragmentToAlertActionIntroFragment(
                    args.groupId,
                    viewModel.getWalletId(),
                    alert
                )
            )
        } else if (alert.type == AlertType.CREATE_INHERITANCE_PLAN_SUCCESS) {
            viewModel.getInheritance(args.walletId.orEmpty(), args.groupId)
        } else if (alert.type == AlertType.GROUP_WALLET_SETUP) {
            if (alert.payload.claimKey) {
                findNavController().navigate(
                    GroupDashboardFragmentDirections.actionGroupDashboardFragmentToClaimKeyFragment(
                        groupId = args.groupId,
                        walletId = viewModel.getWalletId()
                    )
                )
            } else {
                viewModel.handleRegisterSigners(alert.payload.xfps)
            }
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
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.SET_UP_INHERITANCE -> {
                if (viewModel.state.value.isSetupInheritance) {
                    enterPasswordDialog(TargetAction.UPDATE_INHERITANCE_PLAN)
                } else {
                    navigator.openInheritancePlanningScreen(
                        walletId = viewModel.getWalletId(),
                        activityContext = requireContext(),
                        flowInfo = InheritancePlanFlow.SETUP,
                        groupId = args.groupId
                    )
                }
            }

            SheetOptionType.TYPE_PLATFORM_KEY_POLICY -> {
                enterPasswordDialog(TargetAction.UPDATE_SERVER_KEY)
            }

            SheetOptionType.TYPE_EMERGENCY_LOCKDOWN -> {

            }

            SheetOptionType.TYPE_RECURRING_PAYMENT -> {

            }

            SheetOptionType.TYPE_GROUP_CHAT_HISTORY -> {
                viewModel.getGroupChatHistoryPeriod()
            }
        }
    }

    private fun showMoreOptions() {
        val options = mutableListOf<SheetOption>()
        if (viewModel.isPendingCreateWallet().not()) {
            if (viewModel.state.value.myRole.isMasterOrAdmin &&
                viewModel.state.value.group?.walletConfig?.toGroupWalletType()?.isPro == true
            ) {
                options.add(
                    SheetOption(
                        type = SheetOptionType.SET_UP_INHERITANCE,
                        stringId = if (viewModel.state.value.isSetupInheritance) R.string.nc_view_inheritance_plan else R.string.nc_set_up_inheritance_plan
                    ),
                )
                if (!args.walletId.isNullOrEmpty()) {
                    options.add(SheetOption(
                        type = SheetOptionType.TYPE_PLATFORM_KEY_POLICY,
                        stringId = R.string.nc_cosigning_policies
                    ))
                }
            }
            options.addAll(
                mutableListOf(
                    SheetOption(
                        type = SheetOptionType.TYPE_EMERGENCY_LOCKDOWN,
                        stringId = R.string.nc_emergency_lockdown
                    ),
                    SheetOption(
                        type = SheetOptionType.TYPE_RECURRING_PAYMENT,
                        stringId = R.string.nc_view_recurring_payments
                    )
                )
            )
        }
        if (viewModel.groupChat() != null) {
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_GROUP_CHAT_HISTORY,
                    stringId = R.string.nc_manage_group_chat_history
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