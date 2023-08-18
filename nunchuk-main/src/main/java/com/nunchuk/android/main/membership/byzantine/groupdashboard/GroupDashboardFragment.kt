package com.nunchuk.android.main.membership.byzantine.groupdashboard

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.formatDate
import com.nunchuk.android.core.util.fromMxcUriToMatrixDownloadUrl
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.main.membership.byzantine.groupchathistory.GroupChatHistoryFragment
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.AlertType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isInheritanceType
import com.nunchuk.android.model.byzantine.toTitle
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GroupDashboardFragment : MembershipFragment(), BottomSheetOptionListener {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: GroupDashboardFragmentArgs by navArgs()

    private val viewModel: GroupDashboardViewModel by activityViewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                requireActivity().finish()
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
                            viewModel.createGroupChat()
                        }
                    },
                    onMoreClick = {
                        showMoreOptions()
                    },
                    onOpenHealthCheckScreen = {
                        val walletId = args.walletId.orEmpty()
                        if (walletId.isNotEmpty()) {
                            findNavController().navigate(
                                GroupDashboardFragmentDirections.actionGroupDashboardFragmentToHealthCheckFragment(
                                    groupId = args.groupId,
                                    walletId = args.walletId.orEmpty()
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
        }
        flowObserver(viewModel.event) { event ->
            when (event) {
                is GroupDashboardEvent.Error -> showError(message = event.message)
                is GroupDashboardEvent.Loading -> showOrHideLoading(event.loading)
                is GroupDashboardEvent.NavigateToGroupChat -> openRoomChat()
                is GroupDashboardEvent.GetHistoryPeriodSuccess -> {
                    findNavController().navigate(
                        GroupDashboardFragmentDirections.actionGroupDashboardFragmentToGroupChatHistoryFragment(
                            periods = event.periods.toTypedArray(),
                            groupId = viewModel.getByzantineGroup()?.id.orEmpty(),
                            historyPeriodId = viewModel.groupChat()?.historyPeriod?.id.orEmpty()
                        )
                    )
                }

                else -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAlerts()
    }

    private fun alertClick(alert: Alert, role: AssistedWalletRole) {
        if (alert.type == AlertType.GROUP_WALLET_PENDING) {
            if (role == AssistedWalletRole.MASTER) {
                navigator.openMembershipActivity(
                    launcher = launcher,
                    activityContext = requireActivity(),
                    groupStep = MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS,
                    groupId = args.groupId,
                    walletId = args.walletId
                )
            } else {
                navigator.openMembershipActivity(
                    launcher = launcher,
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
                    walletId = args.walletId.orEmpty(),
                    groupId = args.groupId,
                    dummyTransactionId = alert.payload.dummyTransactionId,
                )
            }
        } else if (alert.type.isInheritanceType()) {
            navigator.openInheritancePlanningScreen(
                walletId = args.walletId.orEmpty(),
                activityContext = requireActivity(),
                flowInfo = InheritancePlanFlow.SIGN_DUMMY_TX,
                isOpenFromWizard = false,
                groupId = args.groupId,
                dummyTransactionId = alert.payload.dummyTransactionId
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
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.SET_UP_INHERITANCE -> {

            }

            SheetOptionType.TYPE_PLATFORM_KEY_POLICY -> {

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
        val options = mutableListOf(
            SheetOption(
                type = SheetOptionType.SET_UP_INHERITANCE,
                stringId = R.string.nc_view_inheritance_plan
            ),
            SheetOption(
                type = SheetOptionType.TYPE_PLATFORM_KEY_POLICY,
                stringId = R.string.nc_cosigning_policies
            ),
            SheetOption(
                type = SheetOptionType.TYPE_EMERGENCY_LOCKDOWN,
                stringId = R.string.nc_emergency_lockdown
            ),
            SheetOption(
                type = SheetOptionType.TYPE_RECURRING_PAYMENT,
                stringId = R.string.nc_view_recurring_payments
            )
        )
        if (viewModel.groupChat() != null) {
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_GROUP_CHAT_HISTORY,
                    stringId = R.string.nc_manage_group_chat_history
                )
            )
        }
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun GroupDashboardContent(
    uiState: GroupDashboardState = GroupDashboardState(),
    isEnableStartGroupChat: Boolean = false,
    onGroupChatClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onAlertClick: (alert: Alert, role: AssistedWalletRole) -> Unit = { _, _ -> },
    onMoreClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onDismissClick: (String) -> Unit = {},
    onOpenHealthCheckScreen: () -> Unit = {},
) {
    val master = uiState.group?.members?.find { it.role == AssistedWalletRole.MASTER.name }
    val listState = rememberLazyListState()
    val fabVisibility by remember {
        derivedStateOf {
            listState.isScrollInProgress.not()
        }
    }

    NunchukTheme(statusBarColor = colorResource(id = R.color.nc_grey_light)) {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    backgroundColor = colorResource(id = R.color.nc_grey_light),
                    title = uiState.wallet.name,
                    textStyle = NunchukTheme.typography.titleLarge,
                    elevation = 0.dp,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                        if (uiState.wallet.name.isNotEmpty()) {
                            IconButton(onClick = onWalletClick) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_wallets),
                                    contentDescription = "Wallet icon"
                                )
                            }
                        }
                        IconButton(onClick = onMoreClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    })
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = fabVisibility,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    CompositionLocalProvider(
                        LocalRippleTheme provides
                                if (isEnableStartGroupChat) LocalRippleTheme.current else NoRippleTheme
                    ) {
                        if (uiState.groupChat != null) {
                            FloatingActionButton(onClick = onGroupChatClick) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_messages),
                                    contentDescription = "Search"
                                )
                            }
                        } else {
                            ExtendedFloatingActionButton(onClick = {
                                if (isEnableStartGroupChat) onGroupChatClick()
                            },
                                backgroundColor = if (isEnableStartGroupChat) MaterialTheme.colors.secondary else colorResource(
                                    id = R.color.nc_whisper_color
                                ),
                                text = {
                                    Text(
                                        text = "Start group chat",
                                        color = if (isEnableStartGroupChat) Color.White else colorResource(
                                            id = R.color.nc_grey_dark_color
                                        )
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_create_message),
                                        contentDescription = "Search",
                                        tint = if (isEnableStartGroupChat) LocalContentColor.current.copy(
                                            alpha = LocalContentAlpha.current
                                        ) else colorResource(
                                            id = R.color.nc_grey_dark_color
                                        )
                                    )
                                })
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (uiState.group == null) return@Scaffold
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .background(colorResource(id = R.color.nc_grey_light))
                        .padding(top = 16.dp),
                    state = listState
                ) {
                    AlertListView(
                        alerts = uiState.alerts,
                        currentUserRole = uiState.myRole,
                        onAlertClick = onAlertClick,
                        onDismissClick = onDismissClick
                    )
                    if (uiState.keyStatus.isNotEmpty()) {
                        HealthCheckStatusView(
                            onOpenHealthCheckScreen = onOpenHealthCheckScreen,
                            uiState.signers,
                            uiState.keyStatus
                        )
                    }
                    MemberListView(
                        group = uiState.group,
                        currentUserRole = uiState.myRole,
                        master = master,
                        padTop = if (uiState.alerts.isNotEmpty()) 24.dp else 0.dp,
                        onEditClick = onEditClick
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colors.surface)
                )
            }
        }
    }
}

private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f, 0.0f, 0.0f, 0.0f)
}

private fun LazyListScope.AlertListView(
    alerts: List<Alert>,
    currentUserRole: AssistedWalletRole = AssistedWalletRole.NONE,
    onAlertClick: (Alert, AssistedWalletRole) -> Unit,
    onDismissClick: (String) -> Unit = {},
) {
    if (alerts.isNotEmpty()) {
        item(key = "alert title") {
            Row(
                modifier = Modifier.padding(
                    bottom = 12.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_alert),
                    contentDescription = ""
                )
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 8.dp),
                    text = stringResource(R.string.nc_alert),
                    style = NunchukTheme.typography.title
                )
            }
        }
    }
    items(alerts, key = { "${it.id}${it.body}" }) {
        AlertView(
            isDismissible = it.viewable.not(),
            title = it.title,
            keyText = it.body,
            timeText = (it.createdTimeMillis / 1000).formatDate(),
            onViewClick = {
                onAlertClick(it, currentUserRole)
            },
            onDismissClick = {
                onDismissClick(it.id)
            }
        )
    }
}

private fun LazyListScope.MemberListView(
    currentUserRole: AssistedWalletRole = AssistedWalletRole.NONE,
    master: ByzantineMember? = null,
    group: ByzantineGroup,
    padTop: Dp = 0.dp,
    onEditClick: () -> Unit = {},
) {
    item {
        Row(
            modifier = Modifier
                .padding(top = padTop)
                .background(Color.White)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Image(
                    painter = painterResource(id = R.drawable.ic_account_member),
                    contentDescription = ""
                )
                Text(
                    modifier = Modifier.padding(
                        top = 0.dp,
                        start = 8.dp,
                        end = 16.dp
                    ),
                    text = stringResource(R.string.nc_members),
                    style = NunchukTheme.typography.title
                )
            }

            if (currentUserRole == AssistedWalletRole.MASTER || currentUserRole == AssistedWalletRole.ADMIN) {
                Text(
                    modifier = Modifier.clickable {
                        onEditClick()
                    },
                    text = stringResource(id = R.string.nc_edit),
                    style = NunchukTheme.typography.title,
                    textDecoration = TextDecoration.Underline,
                    color = colorResource(id = R.color.nc_primary_color)
                )
            }
        }
    }

    item {
        master?.let {
            ContactMemberView(
                email = it.user?.email.orEmpty(),
                name = it.user?.name.orEmpty(),
                role = it.role,
                avatarUrl = it.user?.avatar.orEmpty()
            )
        }
    }

    item {
        Divider(
            color = colorResource(id = R.color.nc_whisper_color),
            modifier = Modifier
                .background(color = Color.White)
                .padding(8.dp)
        )
    }

    itemsIndexed(group.members.filter { it.role != AssistedWalletRole.MASTER.name }) { _, member ->
        ContactMemberView(
            email = member.user?.email ?: member.emailOrUsername,
            name = member.user?.name.orEmpty(),
            role = member.role,
            avatarUrl = member.user?.avatar.orEmpty(),
            isPendingMember = member.isContact().not(),
        )
    }
}

@Composable
private fun AlertView(
    isDismissible: Boolean = true,
    title: String = "",
    keyText: String = "",
    timeText: String = "",
    onViewClick: () -> Unit = {},
    onDismissClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(12.dp))
            .border(
                width = 1.dp, color = NcColor.border, shape = RoundedCornerShape(12.dp)
            )
            .background(color = NcColor.white)
            .padding(12.dp)
            .clickable {
                onViewClick()
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1.0f, true)) {
                Text(
                    text = title,
                    style = NunchukTheme.typography.titleSmall
                )
                Text(
                    text = keyText,
                    style = NunchukTheme.typography.bodySmall
                )
                Text(
                    text = timeText,
                    style = NunchukTheme.typography.bodySmall.copy(colorResource(id = R.color.nc_grey_dark_color))
                )
            }

            NcPrimaryDarkButton(
                modifier = Modifier
                    .defaultMinSize(minWidth = 72.dp)
                    .height(36.dp)
                    .padding(start = 12.dp),
                onClick = {
                    if (isDismissible) onDismissClick() else onViewClick()
                }
            ) {
                Text(text = if (isDismissible) "Dismiss" else "View")
            }
        }
    }
}

@Composable
private fun ContactMemberView(
    email: String = "",
    name: String = "",
    role: String = AssistedWalletRole.NONE.name,
    avatarUrl: String = "",
    isPendingMember: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPendingMember.not()) {
                Box(
                    modifier = Modifier
                        .size(48.dp, 48.dp)
                        .clip(CircleShape)
                        .background(color = colorResource(id = R.color.nc_beeswax_light)),
                    contentAlignment = Alignment.Center
                ) {
                    GlideImage(
                        imageModel = { avatarUrl.fromMxcUriToMatrixDownloadUrl() },
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        ),
                        loading = {
                            Text(
                                text = name.shorten(),
                                style = NunchukTheme.typography.title
                            )
                        },
                        failure = {
                            Text(
                                text = name.shorten(),
                                style = NunchukTheme.typography.title
                            )
                        }
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = name,
                        style = NunchukTheme.typography.body
                    )
                    NcTag(
                        modifier = Modifier.padding(top = 4.dp),
                        label = role.toTitle()
                    )
                    Text(
                        modifier = Modifier,
                        text = email,
                        style = NunchukTheme.typography.bodySmall
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .weight(1f, false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp, 48.dp)
                                .clip(CircleShape)
                                .background(color = colorResource(id = R.color.nc_whisper_color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_account_member),
                                contentDescription = ""
                            )
                        }

                        Column(
                            modifier = Modifier
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = email,
                                style = NunchukTheme.typography.body,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            NcTag(
                                modifier = Modifier.padding(top = 4.dp),
                                label = role.toTitle()
                            )
                        }
                    }
                    Text(
                        text = "Pending",
                        style = NunchukTheme.typography.title
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GroupDashboardScreenPreview() {
    GroupDashboardContent()
}