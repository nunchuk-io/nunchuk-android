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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.formatDate
import com.nunchuk.android.core.util.fromMxcUriToMatrixDownloadUrl
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toTitle
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GroupDashboardFragment : MembershipFragment() {

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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                GroupDashboardScreen(viewModel, onEditClick = {
                    findNavController().navigate(
                        GroupDashboardFragmentDirections.actionGroupDashboardFragmentToByzantineInviteMembersFragment(
                            members = viewModel.getAssistedMembers().toTypedArray(),
                            groupId = viewModel.getGroupId(),
                            flow = ByzantineMemberFlow.EDIT,
                        )
                    )
                }, onAlertClick = {
                    alertClick(it)
                }, onWalletClick = {
                    args.walletId?.let {
                        navigator.openWalletDetailsScreen(
                            activityContext = requireActivity(),
                            walletId = it
                        )
                    }
                })
            }
        }
    }

    private fun alertClick(type: String) {
        if (type == "GROUP_WALLET_PENDING") {
            navigator.openMembershipActivity(
                launcher = launcher,
                activityContext = requireActivity(),
                groupStep = MembershipStage.NONE,
                groupId = args.groupId
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is GroupDashboardEvent.Error -> showError(message = event.message)
                is GroupDashboardEvent.Loading -> showOrHideLoading(event.loading)
            }
        }
    }
}

@Composable
private fun GroupDashboardScreen(
    viewModel: GroupDashboardViewModel = viewModel(),
    onEditClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onAlertClick: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupDashboardContent(
        group = state.group,
        currentUserRole = viewModel.currentUserRole(),
        walletName = state.walletExtended.wallet.name,
        alerts = state.alerts,
        onEditClick = onEditClick,
        onWalletClick = onWalletClick,
        onAlertClick = onAlertClick
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun GroupDashboardContent(
    group: ByzantineGroup? = null,
    alerts: List<Alert> = emptyList(),
    walletName: String = "",
    currentUserRole: String = "",
    onEditClick: () -> Unit = {},
    onAlertClick: (String) -> Unit = {},
    onMoreClick: () -> Unit = {},
    onWalletClick: () -> Unit = {}
) {

    val master = group?.members?.find { it.role == AssistedWalletRole.MASTER.name }

    NunchukTheme(statusBarColor = colorResource(id = R.color.nc_grey_light)) {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    backgroundColor = colorResource(id = R.color.nc_grey_light),
                    title = walletName,
                    textStyle = NunchukTheme.typography.titleLarge,
                    elevation = 0.dp,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                        if (walletName.isNotEmpty()) {
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
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(onClick = {}) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_messages),
                            contentDescription = "Search"
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (group == null) return@Scaffold
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .background(colorResource(id = R.color.nc_grey_light))
                        .padding(top = 16.dp)
                ) {
                    if (alerts.isNotEmpty()) {
                        item {
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
                    items(alerts) {
                        AlertView(
                            title = it.title,
                            keyText = it.body,
                            timeText = (it.createdTimeMillis / 1000).formatDate(),
                            onViewClick = {
                                onAlertClick(it.type)
                            }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .padding(top = if (alerts.isNotEmpty()) 24.dp else 0.dp)
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

                            if (currentUserRole == AssistedWalletRole.MASTER.name || currentUserRole == AssistedWalletRole.ADMIN.name) {
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
                                avatarUrl = it.user?.avatar.orEmpty(),
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

                    itemsIndexed(group.members.filter { it.role == AssistedWalletRole.MASTER.name }) { _, member ->
                        ContactMemberView(
                            email = member.user?.email.orEmpty(),
                            name = member.user?.name.orEmpty(),
                            role = member.role,
                            avatarUrl = member.user?.avatar.orEmpty(),
                            isPendingMember = member.isContact().not()
                        )
                    }
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

@Composable
private fun AlertView(
    title: String = "",
    keyText: String = "",
    timeText: String = "",
    onViewClick: () -> Unit = {}
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
                onClick = onViewClick
            ) {
                Text(text = "View")
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
                        label = role.toTitle
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
                                style = NunchukTheme.typography.body
                            )
                            NcTag(
                                modifier = Modifier.padding(top = 4.dp),
                                label = role.toTitle
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
private fun AlertViewPreview() {
    NunchukTheme {
        AlertView()
    }
}

@Preview
@Composable
private fun GroupDashboardScreenPreview() {
    GroupDashboardContent()
}