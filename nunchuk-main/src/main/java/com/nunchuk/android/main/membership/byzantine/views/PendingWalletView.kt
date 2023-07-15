package com.nunchuk.android.main.membership.byzantine.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.everglade
import com.nunchuk.android.compose.ming
import com.nunchuk.android.core.util.fromMxcUriToMatrixDownloadUrl
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineGroupBrief
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.ByzantineWalletConfig
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.User
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.Utils
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun PendingWalletView(
    group: ByzantineGroupBrief? = null,
    walletsExtended: WalletExtended? = null,
    isGroupMasterOrAdmin: Boolean = false,
    hideWalletDetail: Boolean = false,
    isAssistedWallet: Boolean = false,
    badgeCount: Int = 0,
    inviterName: String = "",
    onAccept: () -> Unit = {},
    onDeny: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(8.dp))
            .background(colorResource(id = R.color.nc_grey_light))
            .clickable(onClick = onWalletClick, enabled = walletsExtended != null)
            .fillMaxWidth(),
    ) {
        val colors = if (group != null || isAssistedWallet) {
            listOf(MaterialTheme.colors.ming, MaterialTheme.colors.everglade)
        } else {
            listOf(
                colorResource(id = R.color.nc_primary_light_color),
                colorResource(id = R.color.nc_primary_color)
            )
        }
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            if (walletsExtended == null) {
                Text(
                    text = stringResource(R.string.nc_pending_wallet),
                    style = NunchukTheme.typography.title,
                    color = Color.White
                )
            } else {
                ActiveWallet(
                    walletsExtended = walletsExtended,
                    hideWalletDetail = hideWalletDetail,
                    isAssistedWallet = isAssistedWallet
                )
            }
        }
        if (group != null) {
            Row(
                modifier = Modifier
                    .clickable(
                        onClick = onGroupClick,
                        enabled = isGroupMasterOrAdmin
                    )
                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                if (inviterName.isEmpty()) {
                    WalletAvatar(group = group, badgeCount = badgeCount)
                } else {
                    PendingWalletInviteMember(
                        inviterName = inviterName,
                        onAccept = onAccept,
                        onDeny = onDeny
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.PendingWalletInviteMember(
    inviterName: String,
    onAccept: () -> Unit,
    onDeny: () -> Unit
) {
    Text(
        text = stringResource(
            R.string.nc_pending_wallet_invite_member,
            inviterName
        ),
        style = NunchukTheme.typography.bodySmall,
        modifier = Modifier.weight(1f, fill = true)
    )
    NcPrimaryDarkButton(
        modifier = Modifier
            .defaultMinSize(minWidth = 72.dp)
            .height(36.dp)
            .padding(horizontal = 12.dp),
        onClick = onAccept
    ) {
        Text(text = stringResource(id = R.string.nc_accept))
    }
    NcOutlineButton(
        modifier = Modifier
            .defaultMinSize(minWidth = 72.dp)
            .height(36.dp),
        onClick = onDeny
    ) {
        Text(text = stringResource(id = R.string.nc_deny))
    }
}

@Composable
fun RowScope.WalletAvatar(group: ByzantineGroupBrief, badgeCount: Int = 0) {
    Row(modifier = Modifier.weight(1f, fill = true)) {
        group.members.take(3).forEachIndexed { index, byzantineMember ->
            val padStart = if (index == 0) 0.dp else 4.dp
            AvatarView(
                modifier = Modifier.padding(start = padStart),
                avatarUrl = byzantineMember.avatar.orEmpty(),
                name = byzantineMember.name.orEmpty()
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badgeCount!=0) {
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

        Icon(
            modifier = Modifier.padding(start = 12.dp),
            painter = painterResource(id = R.drawable.ic_arrow_expand),
            contentDescription = "Arrow"
        )
    }
}

@Composable
internal fun ActiveWallet(
    walletsExtended: WalletExtended,
    hideWalletDetail: Boolean,
    isAssistedWallet: Boolean
) {
    val wallet = walletsExtended.wallet
    val balance = "(${wallet.getCurrencyAmount()})"
    Row {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(
                text = wallet.name,
                style = NunchukTheme.typography.title,
                color = Color.White
            )
            Text(
                text = Utils.maskValue(wallet.getBTCAmount(), hideWalletDetail),
                style = NunchukTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = Utils.maskValue(balance, hideWalletDetail),
                style = NunchukTheme.typography.bodySmall,
                color = Color.White
            )
        }

        Column(modifier = Modifier.fillMaxHeight()) {
            Text(
                text = "",
                style = NunchukTheme.typography.title,
                color = Color.White
            )
            Badge {
                if (walletsExtended.isShared || isAssistedWallet) {
                    Icon(
                        modifier = Modifier.padding(start = 8.dp),
                        painter = painterResource(id = R.drawable.ic_wallet_small),
                        contentDescription = "Wallet"
                    )
                }
                val walletTypeName = if (isAssistedWallet) {
                    Utils.maskValue(
                        stringResource(R.string.nc_assisted),
                        hideWalletDetail
                    )
                } else {
                    Utils.maskValue(
                        stringResource(R.string.nc_text_shared),
                        hideWalletDetail
                    )
                }
                Text(
                    modifier = Modifier.padding(
                        start = 4.dp, end = 8.dp,
                    ),
                    text = walletTypeName,
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                )
            }

            Badge(modifier = Modifier.padding(top = 4.dp)) {
                val requireSigns = wallet.totalRequireSigns
                val totalSigns = wallet.signers.size
                val text = if (hideWalletDetail) {
                    '\u2022'.toString().repeat(6)
                } else if (totalSigns == 0 || requireSigns == 0) {
                    stringResource(R.string.nc_wallet_not_configured)
                } else if (totalSigns == 1 && requireSigns == 1) {
                    stringResource(R.string.nc_wallet_single_sig)
                } else {
                    "$requireSigns/$totalSigns ${stringResource(R.string.nc_wallet_multisig)}"
                }
                Text(
                    modifier = Modifier.padding(
                        start = 8.dp, end = 8.dp
                    ),
                    text = text,
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                )
            }
        }
    }
}

@Composable
internal fun Badge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.background,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.background(
            color = backgroundColor, shape = RoundedCornerShape(20.dp)
        ), verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
fun AvatarView(modifier: Modifier = Modifier, avatarUrl: String = "", name: String = "") {
    Box(
        modifier = modifier
            .size(36.dp, 36.dp)
            .clip(CircleShape)
            .background(color = colorResource(id = R.color.nc_beeswax_light)),
        contentAlignment = Alignment.Center
    ) {
        GlideImage(imageModel = { avatarUrl.fromMxcUriToMatrixDownloadUrl() },
            imageOptions = ImageOptions(
                contentScale = ContentScale.Crop, alignment = Alignment.Center
            ),
            loading = {
                Text(
                    text = name.shorten(), style = NunchukTheme.typography.title
                )
            },
            failure = {
                Text(
                    text = name.shorten(), style = NunchukTheme.typography.title
                )
            })
    }
}

@Preview(showBackground = true)
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
            ),
            ByzantineMember(
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
            ),
            ByzantineMember(
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
        walletConfig = ByzantineWalletConfig(
            m = 2,
            n = 4,
            requiredServerKey = true,
            allowInheritance = true
        )
    )
    NunchukTheme {
//        Column {
//            PendingWalletView(
//                walletsExtended = walletsExtended,
//                group = group
//            )
//        }
    }
}