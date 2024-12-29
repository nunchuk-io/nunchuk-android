package com.nunchuk.android.main.groupwallet

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.provider.WalletExtendedProvider
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.getButtonText
import com.nunchuk.android.main.membership.model.getLabel
import com.nunchuk.android.main.membership.model.resId
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.wallet.util.toReadableString

const val freeGroupWalletRoute = "free_group_wallet_route"

fun NavGraphBuilder.freeGroupWallet(
    onEditClicked: () -> Unit = {},
) {
    composable(freeGroupWalletRoute) {

        FreeGroupWalletScreen(
            keys = emptyList(),
            onAddClicked = {},
            onVerifyClicked = {},
            onMoreClicked = {},
            onContinueClicked = {},
            onEditClicked = onEditClicked
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGroupWalletScreen(
    keys: List<AddKeyData> = emptyList(),
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onEditClicked: () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
            CenterAlignedTopAppBar(
                modifier = Modifier,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.textPrimary
                        )
                    }
                },


                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.nc_setup_group_wallet),
                            style = NunchukTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            NcIcon(
                                painter = painterResource(id = R.drawable.ic_encrypted),
                                contentDescription = "Encrypted icon",
                                tint = colorResource(id = R.color.nc_text_secondary)
                            )
                            Text(
                                text = stringResource(id = R.string.nc_encrypted),
                                style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                },
                actions = {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.textPrimary) {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_wallet_create_wallet))
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WalletInfo(
                        walletsExtended = WalletExtended(),
                        onEditClicked = onEditClicked
                    )
                }

                items(keys) { key ->
                    AddKeyCard(
                        item = key,
                        onAddClicked = onAddClicked,
                        onVerifyClicked = onVerifyClicked,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AddKeyCard(
    item: AddKeyData,
    isMissingBackup: Boolean = false,
    modifier: Modifier = Modifier,
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    isDisabled: Boolean = false
) {
    if (item.signer != null) {
        Box(
            modifier = modifier.background(
                color = if (item.verifyType != VerifyType.NONE) {
                    colorResource(id = R.color.nc_fill_slime)
                } else if (isDisabled) {
                    colorResource(id = R.color.nc_grey_dark_color)
                } else {
                    colorResource(id = R.color.nc_fill_beewax)
                },
                shape = RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                NcCircleImage(
                    resId = item.signer.toReadableDrawableResId(),
                )
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = item.signer.name,
                        style = NunchukTheme.typography.body
                    )
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        NcTag(
                            label = item.signer.toReadableSignerType(context = LocalContext.current),
                            backgroundColor = colorResource(
                                id = R.color.nc_bg_mid_gray
                            ),
                        )
                        if (item.signer.isShowAcctX()) {
                            NcTag(
                                modifier = Modifier.padding(start = 4.dp),
                                label = stringResource(R.string.nc_acct_x, item.signer.index),
                                backgroundColor = colorResource(
                                    id = R.color.nc_bg_mid_gray
                                ),
                            )
                        }
                    }
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = item.signer.getXfpOrCardIdLabel(),
                        style = NunchukTheme.typography.bodySmall
                    )
                }
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
            }
        }
    } else {
        if (item.verifyType != VerifyType.NONE) {
            Box(
                modifier = modifier.background(
                    colorResource(id = R.color.nc_fill_slime),
                    shape = RoundedCornerShape(8.dp)
                ),
                contentAlignment = Alignment.Center,
            ) {
                ConfigItem(item, isDisabled = isDisabled)
            }
        } else {
            NcDashLineBox(modifier = modifier) {
                ConfigItem(item, onAddClicked, isDisabled = isDisabled)
            }
        }
    }
}

@Composable
private fun ConfigItem(
    item: AddKeyData,
    onAddClicked: ((data: AddKeyData) -> Unit)? = null,
    isDisabled: Boolean = false
) {
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
                text = item.type.getLabel(LocalContext.current),
                style = NunchukTheme.typography.body
            )
            Row(modifier = Modifier.padding(top = 4.dp)) {
                if (item.type.isAddInheritanceKey) {
                    NcTag(
                        label = stringResource(R.string.nc_inheritance),
                        backgroundColor = colorResource(
                            id = R.color.nc_bg_mid_gray
                        ),
                    )
                }
                if (item.signer?.isShowAcctX() == true) {
                    NcTag(
                        modifier = Modifier.padding(start = if (item.type == MembershipStep.HONEY_ADD_INHERITANCE_KEY) 4.dp else 0.dp),
                        label = stringResource(R.string.nc_acct_x, item.signer.index),
                        backgroundColor = colorResource(
                            id = R.color.nc_bg_mid_gray
                        ),
                    )
                }
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

@Composable
internal fun WalletInfo(
    walletsExtended: WalletExtended,
    onEditClicked: () -> Unit = {},
) {
    val wallet = walletsExtended.wallet

    val requireSigns = wallet.totalRequireSigns
    val totalSigns = wallet.signers.size
    Box(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colorResource(id = R.color.cl_084B7B),
                        colorResource(id = R.color.cl_2B74A9)
                    ), start = Offset.Zero, end = Offset.Infinite
                )
            )
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_group_wallet),
                        style = NunchukTheme.typography.titleLarge
                            .copy(color = colorResource(id = R.color.nc_white_color))
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(end = 8.dp),
                            text = "$requireSigns/$totalSigns ${stringResource(R.string.nc_wallet_multisig)}",
                            style = NunchukTheme.typography.bodySmall
                                .copy(color = colorResource(id = R.color.nc_white_color))
                        )

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(shape = CircleShape)
                                .background(colorResource(id = R.color.nc_grey_light))
                        )

                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = wallet.addressType.toReadableString(LocalContext.current),
                            style = NunchukTheme.typography.bodySmall.copy(
                                color = colorResource(
                                    id = R.color.nc_white_color
                                )
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(50.dp))
                        .background(colorResource(id = R.color.nc_white_color))
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                        .clickable { onEditClicked() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(2.dp),
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Wallet icon",
                        tint = Color(0xFF031F2B)
                    )

                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(id = R.string.nc_edit),
                        style = NunchukTheme.typography.titleSmall.copy(
                            color = Color(0xFF031F2B)
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 0.5.dp,
                color = colorResource(id = R.color.nc_bg_mid_gray)
            )

            Row(
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_chain),
                        contentDescription = "Wallet icon",
                        tint = colorResource(id = R.color.nc_white_color)
                    )

                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(id = R.string.nc_copy_wallet_link),
                        style = NunchukTheme.typography.titleSmall.copy(color = colorResource(id = R.color.nc_white_color))
                    )
                }

                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_qrcode_2),
                        contentDescription = "Wallet icon",
                        tint = colorResource(id = R.color.nc_white_color)
                    )

                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(id = R.string.nc_show_qr),
                        style = NunchukTheme.typography.titleSmall.copy(color = colorResource(id = R.color.nc_white_color))
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WalletInfoPreview(
    @PreviewParameter(WalletExtendedProvider::class) walletsExtended: WalletExtended,
) {
    NunchukTheme {
        WalletInfo(walletsExtended)
    }
}

@PreviewLightDark
@Composable
private fun GroupWalletScreenPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        FreeGroupWalletScreen()
    }
}

