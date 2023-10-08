package com.nunchuk.android.main.membership.byzantine.groupdashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.formatDate
import com.nunchuk.android.core.util.fromMxcUriToMatrixDownloadUrl
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.toTitle
import com.nunchuk.android.type.SignerType
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun GroupDashboardContent(
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
    val isKeyholderLimited = uiState.myRole == AssistedWalletRole.KEYHOLDER_LIMITED
    val fabVisibility by remember {
        derivedStateOf {
            listState.isScrollInProgress.not()
        }
    }

    val signers by remember(uiState.myRole, uiState.signers) {
        derivedStateOf {
            if (uiState.myRole.isKeyHolderLimited) {
                uiState.signers.filter { it.isVisible }.filter { it.type != SignerType.SERVER }
            } else {
                uiState.signers.filter { it.type != SignerType.SERVER }
            }
        }
    }

    val isShowMore: Boolean = (uiState.groupChat != null && uiState.myRole.isMasterOrAdmin)
            || if (uiState.group?.isPendingWallet() == true) {
        uiState.myRole == AssistedWalletRole.MASTER
    } else {
        uiState.myRole.isMasterOrAdmin &&
                uiState.group?.walletConfig?.toGroupWalletType()?.isPro == true
    }

    NunchukTheme(statusBarColor = colorResource(id = R.color.nc_grey_light)) {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    backgroundColor = colorResource(id = R.color.nc_grey_light),
                    title = uiState.wallet.name.ifEmpty {
                        stringResource(
                            R.string.nc_pending_wallet
                        )
                    },
                    textStyle = NunchukTheme.typography.titleLarge,
                    elevation = 0.dp,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                        if (isKeyholderLimited.not()) {
                            val isWalletCreated = uiState.wallet.name.isNotEmpty()
                            IconButton(onClick = onWalletClick, enabled = isWalletCreated) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_wallets),
                                    contentDescription = "Wallet icon",
                                    tint = if (isWalletCreated) colorResource(id = R.color.nc_primary_color) else colorResource(
                                        id = R.color.nc_boulder_color
                                    )
                                )
                            }
                            if (isShowMore) {
                                IconButton(onClick = onMoreClick) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_more),
                                        contentDescription = "More icon"
                                    )
                                }
                            }
                        }
                    })
            },
            floatingActionButton = {
                if (isKeyholderLimited.not()) {
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
                    alertListView(
                        alerts = uiState.alerts,
                        currentUserRole = uiState.myRole,
                        onAlertClick = onAlertClick,
                        onDismissClick = onDismissClick
                    )
                    if (uiState.keyStatus.isNotEmpty()) {
                        HealthCheckStatusView(
                            onOpenHealthCheckScreen = onOpenHealthCheckScreen,
                            signers = signers,
                            status = uiState.keyStatus
                        )
                    }
                    if (isKeyholderLimited.not()) {
                        memberListView(
                            group = uiState.group,
                            currentUserRole = uiState.myRole,
                            master = master,
                            padTop = if (uiState.alerts.isNotEmpty()) 24.dp else 0.dp,
                            onEditClick = onEditClick
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

private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f, 0.0f, 0.0f, 0.0f)
}

private fun LazyListScope.alertListView(
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

private fun LazyListScope.memberListView(
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
                if (keyText.isNotEmpty()) {
                    Text(
                        text = keyText,
                        style = NunchukTheme.typography.bodySmall
                    )
                }
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