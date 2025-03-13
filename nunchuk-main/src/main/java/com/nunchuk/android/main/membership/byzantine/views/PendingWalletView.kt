package com.nunchuk.android.main.membership.byzantine.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.ActiveWallet
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.compose.getWalletColors
import com.nunchuk.android.compose.isLimitAccess
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.fromMxcUriToMatrixDownloadUrl
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.User
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.byzantine.isMasterOrAdminOrFacilitatorAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.healthCheckTimeColor
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun PendingWalletView(
    group: ByzantineGroup? = null,
    sandbox: GroupSandbox? = null,
    isSandboxWallet: Boolean = false,
    walletsExtended: WalletExtended? = null,
    hideWalletDetail: Boolean = false,
    isAssistedWallet: Boolean = false,
    role: String = AssistedWalletRole.OBSERVER.name,
    badgeCount: Int = 0,
    inviterName: String = "",
    isLocked: Boolean = false,
    primaryOwnerMember: ByzantineMember? = null,
    signers: List<SignerModel> = emptyList(),
    status: Map<String, KeyHealthStatus> = emptyMap(),
    useLargeFont: Boolean = false,
    walletStatus: String? = null,
    showShortcuts: Boolean = false,
    onAccept: () -> Unit = {},
    onDeny: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onSendClick: () -> Unit = {},
    onReceiveClick: () -> Unit = {},
    onOpenFreeGroupWallet: (GroupSandbox) -> Unit = {},
    isDeprecatedGroupWallet: Boolean = false,
) {
    val isLimitAccess = isDeprecatedGroupWallet || isLimitAccess(group, role, walletStatus)
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(8.dp))
            .background(colorResource(id = R.color.nc_grey_light))
            .clickable(onClick = onWalletClick, enabled = walletsExtended != null)
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = getWalletColors(
                            wallet = walletsExtended?.wallet,
                            isAssistedWallet = isAssistedWallet,
                            isLimitAccess = isLimitAccess,
                            isJoined = inviterName.isEmpty(),
                            hasGroup = group != null,
                            isFreeGroupWallet = isSandboxWallet
                        ), start = Offset.Zero, end = Offset.Infinite
                    )
                )
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            if (inviterName.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.nc_wallet_invitation),
                    style = NunchukTheme.typography.title,
                    color = MaterialTheme.colorScheme.textPrimary
                )
            } else if (walletsExtended == null) {
                if (sandbox != null) {
                    Text(
                        text = sandbox.name,
                        style = NunchukTheme.typography.title,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.nc_pending_wallet),
                        style = NunchukTheme.typography.title,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                }
            } else {
                Column {
                    ActiveWallet(
                        walletsExtended = walletsExtended,
                        hideWalletDetail = hideWalletDetail,
                        isAssistedWallet = isAssistedWallet,
                        role = role,
                        useLargeFont = useLargeFont,
                        walletStatus = walletStatus.orEmpty(),
                        isSandboxWallet = isSandboxWallet == true,
                        isDeprecatedGroupWallet = isDeprecatedGroupWallet
                    )
                    if (showShortcuts) {
                        val allowShowShortcuts =
                            (group == null || role != AssistedWalletRole.KEYHOLDER_LIMITED.name) && isLocked.not()
                                    && walletStatus != WalletStatus.LOCKED.name
                                    && walletStatus != WalletStatus.REPLACED.name
                        if (allowShowShortcuts) {
                            ShortcutsView(
                                isHasBalance = walletsExtended.wallet.balance.value > 0,
                                useLargeFont = useLargeFont,
                                onSendClick = onSendClick,
                                onReceiveClick = onReceiveClick,
                            )
                        }
                    }
                }
            }
        }
        if (group != null && walletStatus != WalletStatus.REPLACED.name) {
            Row(
                modifier = Modifier
                    .clickable(
                        enabled = role != AssistedWalletRole.OBSERVER.name && inviterName.isEmpty() && isLocked.not(),
                        onClick = onGroupClick,
                    )
                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                ByzantineBottomContent(
                    group = group,
                    badgeCount = badgeCount,
                    isLocked = isLocked,
                    role = role,
                    primaryOwnerMember = primaryOwnerMember,
                    inviterName = inviterName,
                    walletStatus = walletStatus,
                    onAccept = onAccept,
                    onDeny = onDeny
                )
            }
        } else if (isAssistedWallet || (walletStatus == WalletStatus.LOCKED.name && isSandboxWallet == false)) {
            Row(
                modifier = Modifier
                    .clickable(
                        enabled = isLocked.not(),
                        onClick = onGroupClick,
                    )
                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                AssistedWalletBottomContent(
                    badgeCount = badgeCount,
                    isLocked = isLocked,
                    signers = signers,
                    status = status,
                    walletStatus = walletStatus
                )
            }
        } else if (sandbox != null && !sandbox.finalized) {
            Row(
                modifier = Modifier
                    .clickable(
                        enabled = isLocked.not(),
                        onClick = { onOpenFreeGroupWallet(sandbox) },
                    )
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nc_setup_in_progress),
                    style = NunchukTheme.typography.bodySmall,
                )

                Spacer(modifier = Modifier.weight(1f))

                NcIcon(
                    painter = painterResource(id = R.drawable.ic_arrow_expand),
                    contentDescription = "Arrow"
                )
            }
        }
    }
}

@Composable
fun RowScope.PendingWalletInviteMember(
    inviterName: String, onAccept: () -> Unit, onDeny: () -> Unit,
) {
    Text(
        text = stringResource(
            R.string.nc_pending_wallet_invite_member, inviterName
        ), style = NunchukTheme.typography.bodySmall, modifier = Modifier
            .weight(1f, fill = true)
            .padding(end = 12.dp)
    )
    Box(
        modifier = Modifier
            .height(36.dp)
            .defaultMinSize(minWidth = 62.dp)
            .background(
                color = Color.Transparent, shape = RoundedCornerShape(48.dp)
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.controlFillPrimary,
                shape = RoundedCornerShape(48.dp)
            )
            .clickable {
                onDeny()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = R.string.nc_deny),
            style = NunchukTheme.typography.captionTitle
        )
    }

    Box(
        modifier = Modifier
            .height(36.dp)
            .padding(start = 12.dp)
            .defaultMinSize(minWidth = 72.dp)
            .background(
                color = MaterialTheme.colorScheme.controlFillPrimary,
                shape = RoundedCornerShape(48.dp)
            )
            .clickable {
                onAccept()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = R.string.nc_accept),
            style = NunchukTheme.typography.captionTitle.copy(
                color = MaterialTheme.colorScheme.controlTextPrimary
            )
        )
    }
}

@Composable
fun AssistedWalletBottomContent(
    badgeCount: Int = 0,
    isLocked: Boolean = false,
    signers: List<SignerModel>,
    status: Map<String, KeyHealthStatus>,
    walletStatus: String? = null,
) {
    if (isLocked) {
        Box(
            modifier = Modifier
                .background(
                    color = colorResource(id = R.color.nc_red_tint_color),
                    shape = RoundedCornerShape(size = 20.dp)
                )
                .padding(start = 10.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.nc_lockdown_in_progress),
                style = NunchukTheme.typography.caption
            )
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status.isNotEmpty() && signers.any { it.type != SignerType.SERVER }) {
                LazyRow(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f, fill = true),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(signers.filter { it.type != SignerType.SERVER }) {
                        NcCircleImage(
                            resId = it.toReadableDrawableResId(),
                            size = 36.dp,
                            iconSize = 18.dp,
                            color = status[it.fingerPrint]?.lastHealthCheckTimeMillis.healthCheckTimeColor(),
                            iconTintColor = colorResource(R.color.nc_grey_g7)
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.nc_dashboard),
                    style = NunchukTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f, fill = true)
                )
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (badgeCount != 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp, 24.dp)
                            .clip(CircleShape)
                            .background(color = colorResource(id = R.color.nc_orange_color)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            style = NunchukTheme.typography.titleSmall.copy(color = Color.White)
                        )
                    }
                }

                if (walletStatus == WalletStatus.LOCKED.name) {
                    LockedBadge()
                }

                NcIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    painter = painterResource(id = R.drawable.ic_arrow_expand),
                    contentDescription = "Arrow"
                )
            }
        }
    }
}

@Composable
fun RowScope.ByzantineBottomContent(
    group: ByzantineGroup,
    badgeCount: Int = 0,
    isLocked: Boolean = false,
    role: String = AssistedWalletRole.NONE.name,
    inviterName: String = "",
    primaryOwnerMember: ByzantineMember? = null,
    walletStatus: String? = null,
    onAccept: () -> Unit = {},
    onDeny: () -> Unit = {},
) {
    if (isLocked) {
        AvatarView(group = group)
        Box(
            modifier = Modifier
                .background(
                    color = colorResource(id = R.color.nc_red_tint_color),
                    shape = RoundedCornerShape(size = 20.dp)
                )
                .padding(start = 10.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.nc_lockdown_in_progress),
                style = NunchukTheme.typography.caption,
                color = colorResource(R.color.nc_blue_primary)
            )
        }
    } else if (inviterName.isNotEmpty()) {
        PendingWalletInviteMember(
            inviterName = inviterName, onAccept = onAccept, onDeny = onDeny
        )
    } else if (role == AssistedWalletRole.OBSERVER.name) {
        Row(
            modifier = Modifier
                .background(
                    color = colorResource(R.color.nc_background_primary),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.whisper,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NcIcon(
                painter = painterResource(id = R.drawable.ic_show_pass),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.nc_observing),
                style = NunchukTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    } else if (role == AssistedWalletRole.KEYHOLDER_LIMITED.name) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nc_dashboard),
                style = NunchukTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .weight(1f, fill = true)
            )

            if (badgeCount != 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp, 24.dp)
                        .clip(CircleShape)
                        .background(color = colorResource(id = R.color.nc_orange_color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = NunchukTheme.typography.titleSmall.copy(color = Color.White)
                    )
                }
            }
            if (walletStatus == WalletStatus.LOCKED.name) {
                LockedBadge()
            }
            NcIcon(
                modifier = Modifier.padding(start = 12.dp),
                painter = painterResource(id = R.drawable.ic_arrow_expand),
                contentDescription = "Arrow"
            )
        }
    } else if (role.toRole.isMasterOrAdminOrFacilitatorAdmin && primaryOwnerMember != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                avatarUrl = primaryOwnerMember.user?.avatar.orEmpty(),
                name = primaryOwnerMember.user?.name.orEmpty(),
                isContact = primaryOwnerMember.isPendingRequest().not()
            )

            Text(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                text = primaryOwnerMember.user?.name ?: primaryOwnerMember.emailOrUsername,
                style = NunchukTheme.typography.titleSmall
            )
            if (walletStatus == WalletStatus.LOCKED.name) {
                LockedBadge()
            }
            if (badgeCount != 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp, 24.dp)
                        .clip(CircleShape)
                        .background(color = colorResource(id = R.color.nc_orange_color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = NunchukTheme.typography.titleSmall.copy(color = Color.White)
                    )
                }
            }

            NcIcon(
                modifier = Modifier.padding(start = 12.dp),
                painter = painterResource(id = R.drawable.ic_arrow_expand),
                contentDescription = "Arrow"
            )
        }
    } else {
        AvatarView(group = group)
        if (walletStatus == WalletStatus.LOCKED.name) {
            LockedBadge()
        }
        if (badgeCount != 0) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp, 24.dp)
                    .clip(CircleShape)
                    .background(color = colorResource(id = R.color.nc_orange_color)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    style = NunchukTheme.typography.titleSmall.copy(color = Color.White)
                )
            }
        }

        NcIcon(
            modifier = Modifier.padding(start = 12.dp),
            painter = painterResource(id = R.drawable.ic_arrow_expand),
            contentDescription = "Arrow"
        )
    }
}

@Composable
fun LockedBadge() {
    Text(
        modifier = Modifier
            .background(
                color = colorResource(id = R.color.nc_red_tint_color),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = stringResource(id = R.string.nc_locked),
        style = NunchukTheme.typography.caption,
        color = colorResource(R.color.nc_blue_primary)
    )
}

@Composable
internal fun RowScope.AvatarView(group: ByzantineGroup) {
    Row(modifier = Modifier.weight(1f, fill = true)) {
        val sortedList =
            group.members.filter { it.role != AssistedWalletRole.OBSERVER.name }
                .sortedWith(compareBy { it.isPendingRequest() })
        sortedList.take(5).forEachIndexed { index, byzantineMember ->
            val padStart = if (index == 0) 0.dp else 4.dp
            AvatarView(
                modifier = Modifier.padding(start = padStart),
                avatarUrl = byzantineMember.user?.avatar.orEmpty(),
                name = byzantineMember.user?.name.orEmpty(),
                isContact = byzantineMember.isPendingRequest().not()
            )
        }
    }
}

@Composable
fun AvatarView(
    modifier: Modifier = Modifier,
    avatarUrl: String = "",
    name: String = "",
    isContact: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(36.dp, 36.dp)
            .clip(CircleShape)
            .background(color = colorResource(id = R.color.nc_beeswax_light)),
        contentAlignment = Alignment.Center
    ) {
        val image: @Composable BoxScope.() -> Unit = {
            if (isContact) {
                Text(
                    text = name.shorten(), style = NunchukTheme.typography.title
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp, 36.dp)
                        .clip(CircleShape)
                        .background(color = colorResource(id = R.color.nc_bg_mid_gray)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_account_member),
                        contentDescription = ""
                    )
                }
            }
        }
        GlideImage(imageModel = {
            if (isContact) {
                avatarUrl.fromMxcUriToMatrixDownloadUrl()
            } else ""
        }, imageOptions = ImageOptions(
            contentScale = ContentScale.Crop, alignment = Alignment.Center
        ), loading = {
            image()
        }, failure = {
            image()
        })
    }
}

@Composable
@Preview
fun ShortcutsView(
    isHasBalance: Boolean = false,
    useLargeFont: Boolean = false,
    onSendClick: () -> Unit = {},
    onReceiveClick: () -> Unit = {},
) {
    Column {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.whisper,
            thickness = 0.5.dp
        )

        Row(
            horizontalArrangement = Arrangement.Absolute.Center,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WalletActionButton(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isHasBalance) 1f else 0.5f),
                text = stringResource(id = R.string.nc_send),
                useLargeFont = useLargeFont,
                onClick = onSendClick,
                clickable = isHasBalance
            )
            VerticalDivider(
                modifier = Modifier.height(16.dp),
                color = MaterialTheme.colorScheme.whisper,
                thickness = 0.5.dp
            )
            WalletActionButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.nc_receive),
                useLargeFont = useLargeFont,
                onClick = onReceiveClick
            )
        }
    }
}

@Composable
fun WalletActionButton(
    modifier: Modifier,
    text: String = "",
    useLargeFont: Boolean = false,
    clickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick, enabled = clickable),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = text,
            style = if (useLargeFont) NunchukTheme.typography.body else NunchukTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

@PreviewLightDark
@Composable
private fun WalletInvitationPreview() {
    NunchukTheme {
        Row(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            PendingWalletInviteMember(
                inviterName = "ThongLe20",
                onAccept = {},
                onDeny = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
fun PendingWalletViewPreview() {
    val walletsExtended = WalletExtended(
        wallet = Wallet(
            id = "h0tj2hzp",
            name = "555AndroidHoney",
            totalRequireSigns = 2,
            signers = listOf(
                SingleSigner(
                    name = "TAPSIGNER #2",
                    xpub = "xpub6BemYiVNp19a2WVjnfsSs9f1hS2QSHknazBuUHYem11NPHZ4qHXbAxEaS5BQVWiB49XbotxC2Jrkz32ooJtHQtWTHrdZT7xPfSkYZzFsCcD",
                    publicKey = "",
                    derivationPath = "m/48h/0h/0h",
                    masterFingerprint = "56a80fae",
                    lastHealthCheck = 0,
                    masterSignerId = "56a80fae",
                    used = false,
                    type = SignerType.NFC,
                    hasMasterSigner = true,
                    descriptor = "[56a80fae/48'/0'/0']xpub6BemYiVNp19a2WVjnfsSs9f1hS2QSHknazBuUHYem11NPHZ4qHXbAxEaS5BQVWiB49XbotxC2Jrkz32ooJtHQtWTHrdZT7xPfSkYZzFsCcD",
                    tags = listOf()
                )
            ),
            addressType = AddressType.NATIVE_SEGWIT,
            escrow = false,
            balance = Amount(value = 22992849, formattedValue = "0.22992849"),
            createDate = 0,
            description = "",
            gapLimit = 20
        ), isShared = false, roomWallet = null
    )
    val group = ByzantineGroup(
        createdTimeMillis = 1687880984000,
        id = "328924802704216064",
        members = listOf(
            ByzantineMember(
                emailOrUsername = "thongle+20@nunchuk.io",
                membershipId = "328924802704216065",
                permissions = listOf(
                    "group.members.write",
                    "wallet.inheritance.write",
                    "server_key.write",
                    "wallet.emergency_lockdown"
                ),
                role = "MASTER",
                status = "ACTIVE",
                inviterUserId = "",
                user = User(
                    id = "328923791713374208",
                    name = "ThongLe20",
                    email = "thongle+20@nunchuk.io",
                    gender = "UNKNOWN",
                    avatar = "",
                    status = "ACTIVATED",
                    chatId = "@nunchuk_io_328923791713374208:nunchuk.io",
                    loginType = "EMAIL",
                    username = ""
                )
            ), ByzantineMember(
                emailOrUsername = "thongle+10@nunchuk.io",
                membershipId = "328924802737770496",
                permissions = listOf(),
                role = "ADMIN",
                status = "PENDING",
                inviterUserId = "328923791713374208",
                user = User(
                    id = "322620755730763776",
                    name = "thongle10",
                    email = "thongle+10@nunchuk.io",
                    gender = "UNKNOWN",
                    avatar = "",
                    status = "ACTIVATED",
                    chatId = "@nunchuk_io_322620755730763776:nunchuk.io",
                    loginType = "EMAIL",
                    username = ""
                )
            ), ByzantineMember(
                emailOrUsername = "thongle+21@nunchuk.io",
                membershipId = "328924802737770497",
                permissions = listOf(),
                role = "KEYHOLDER",
                status = "ACTIVE",
                inviterUserId = "328923791713374208",
                user = User(
                    id = "328924575112892416",
                    name = "Thong Le 21",
                    email = "thongle+21@nunchuk.io",
                    gender = "UNKNOWN",
                    avatar = "mxc://nunchuk.io/tbUWGvXrAxMIxhNIZeuRydnh",
                    status = "ACTIVATED",
                    chatId = "@nunchuk_io_328924575112892416:nunchuk.io",
                    loginType = "EMAIL",
                    username = ""
                )
            )
        ),
        setupPreference = "SINGLE_PERSON",
        status = "PENDING_WALLET",
        isViewPendingWallet = false,
        isLocked = false,
        walletConfig = WalletConfig(
            m = 2, n = 4, requiredServerKey = true, allowInheritance = true
        ),
        slug = MembershipPlan.BYZANTINE_PREMIER.name
    )
    val members = group.members.map {
        ByzantineMember(
            emailOrUsername = it.emailOrUsername,
            role = it.role,
            status = it.status,
            inviterUserId = it.inviterUserId,
            user = it.user,
            membershipId = it.membershipId,
            permissions = it.permissions,
        )
    }
    NunchukTheme {
        Column {
            PendingWalletView(
                walletsExtended = walletsExtended,
                group = ByzantineGroup(
                    id = group.id,
                    status = group.status,
                    createdTimeMillis = group.createdTimeMillis,
                    members = members,
                    isViewPendingWallet = true,
                    walletConfig = group.walletConfig,
                    setupPreference = group.setupPreference,
                    isLocked = false,
                    slug = MembershipPlan.BYZANTINE_PREMIER.name
                ),
                walletStatus = WalletStatus.ACTIVE.name,
                isAssistedWallet = true
            )
        }
    }
}