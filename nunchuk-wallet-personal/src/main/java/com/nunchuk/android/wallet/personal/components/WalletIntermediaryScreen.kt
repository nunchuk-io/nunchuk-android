package com.nunchuk.android.wallet.personal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.wallet.personal.R

@Composable
fun WalletIntermediaryScreen(
    isMembership: Boolean = false,
    remainingAssistedWallets: Int = 0,
    onWalletTypeSelected: (WalletType) -> Unit = {},
    onRecoverWalletClicked: () -> Unit = {},
    onJoinGroupWalletClicked: () -> Unit = {},
    onScanQRClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "",
                    isBack = false
                )
            },
            bottomBar = {
                if (isMembership.not()) Bottom()
            },
        ) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.nc_wallet_create_wallet),
                    style = NunchukTheme.typography.heading,
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(id = R.string.nc_select_wallet_type),
                    style = NunchukTheme.typography.body,
                )

                if (isMembership) {
                    PaidUserWalletTypeContent(
                        remainingAssistedWallets = remainingAssistedWallets,
                        onWalletTypeSelected = onWalletTypeSelected
                    )
                } else {
                    FreeUserWalletTypeContent(
                        onWalletTypeSelected = onWalletTypeSelected
                    )
                }

                Action(
                    onRecoverWalletClicked = onRecoverWalletClicked,
                    onJoinGroupWalletClicked = onJoinGroupWalletClicked,
                    onScanQRClicked = onScanQRClicked
                )
            }
        }
    }
}

@Preview
@Composable
fun PaidUserWalletTypeContent(
    remainingAssistedWallets: Int = 0,
    onWalletTypeSelected: (WalletType) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.border),
                    shape = RoundedCornerShape(8.dp)
                )
                .fillMaxWidth()
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable {
                        onWalletTypeSelected(WalletType.ASSISTED)
                    }
            ) {
                val (banner, content) = createRefs()

                SubscriptionRequiredBanner(
                    modifier = Modifier.constrainAs(banner) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
                )

                WalletTypeItem(
                    modifier = Modifier
                        .constrainAs(content) {
                            top.linkTo(banner.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                    title = stringResource(id = R.string.nc_assisted_wallet),
                    subtext = "($remainingAssistedWallets remaining)",
                    desc = stringResource(R.string.nc_assisted_wallet_create_wallet_desc),
                    resId = R.drawable.ic_wallet_type_assisted_wallet
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.border),
                    shape = RoundedCornerShape(8.dp)
                )
                .fillMaxWidth()
                .clickable {
                    onWalletTypeSelected(WalletType.UNASSISTED)
                }
        ) {
            WalletTypeItem(
                modifier = Modifier.padding(16.dp),
                title = stringResource(id = R.string.nc_unassisted_wallet),
                desc = stringResource(R.string.nc_unassisted_wallet_create_wallet_desc),
                resId = R.drawable.ic_wallet_type_unassisted_wallet
            )
        }
    }
}

@Composable
fun Action(
    onRecoverWalletClicked: () -> Unit = {},
    onJoinGroupWalletClicked: () -> Unit = {},
    onScanQRClicked: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .height(10.dp)
            .background(MaterialTheme.colorScheme.greyLight)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onJoinGroupWalletClicked()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcIcon(
            painter = painterResource(id = R.drawable.ic_group_wallet),
            contentDescription = "Group Wallet Icon"
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(id = R.string.nc_join_group_wallet),
            style = NunchukTheme.typography.body,
        )

        Spacer(modifier = Modifier.weight(1f))

        NcIcon(
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    onScanQRClicked()
                },
            painter = painterResource(id = R.drawable.ic_qr),
            contentDescription = "QR Icon"
        )

        VerticalDivider(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(20.dp),
            color = MaterialTheme.colorScheme.border
        )

        NcIcon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right_new),
            contentDescription = "Arrow Right Icon"
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.border
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                onRecoverWalletClicked()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcIcon(
            painter = painterResource(id = R.drawable.ic_recover_existing_wallet),
            contentDescription = "Group Wallet Icon"
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(id = R.string.nc_text_recover_wallet),
            style = NunchukTheme.typography.body,
        )

        Spacer(modifier = Modifier.weight(1f))

        NcIcon(
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    onRecoverWalletClicked()
                },
            painter = painterResource(id = R.drawable.ic_arrow_right_new),
            contentDescription = "Arrow Right Icon"
        )
    }
}

@Composable
fun Bottom() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.border),
                shape = RoundedCornerShape(8.dp),
            )
            .fillMaxWidth()
            .clickable {
                context.openExternalLink("https://nunchuk.io/individuals")
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_wallet_type_assisted_wallet),
                contentDescription = "Wallet Icon",
                modifier = Modifier
                    .size(44.dp)
            )

            Text(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                text = stringResource(R.string.nc_create_wallet_free_user_guide),
                style = NunchukTheme.typography.titleSmall,
            )

            NcIcon(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(24.dp),
                painter = painterResource(id = R.drawable.ic_arrow_right_new),
                contentDescription = "Arrow Right Icon"
            )
        }
    }
}

@Preview
@Composable
fun FreeUserWalletTypeContent(
    onWalletTypeSelected: (WalletType) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {

        WalletTypeItem(
            modifier = Modifier
                .padding(16.dp)
                .clickable {
                    onWalletTypeSelected(WalletType.CUSTOM)
                },
            title = stringResource(id = R.string.nc_custom_wallet),
            desc = stringResource(R.string.nc_custom_wallet_desc),
            resId = R.drawable.ic_wallet_type_custom_wallet
        )

        WalletTypeItem(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .clickable {
                    onWalletTypeSelected(WalletType.HOT)
                },
            title = stringResource(id = R.string.nc_hot_wallet),
            desc = stringResource(R.string.nc_hot_wallet_create_wallet_desc),
            resId = R.drawable.ic_wallet_type_hot_wallet
        )

        WalletTypeItem(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                .clickable {
                    onWalletTypeSelected(WalletType.GROUP)
                },
            title = stringResource(id = R.string.nc_group_wallet),
            desc = stringResource(R.string.nc_group_wallet_desc),
            resId = R.drawable.ic_wallet_type_group_wallet
        )

        WalletTypeItem(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                .clickable {
                    onWalletTypeSelected(WalletType.DECOY)
                },
            title = stringResource(id = R.string.nc_decoy_wallet),
            desc = stringResource(R.string.nc_decoy_wallet_desc),
            resId = if (NunchukTheme.isDark) R.drawable.ic_wallet_type_decoy_wallet_dark else R.drawable.ic_wallet_type_decoy_wallet
        )

    }
}

@Composable
fun WalletTypeItem(
    modifier: Modifier = Modifier,
    title: String,
    subtext: String = "",
    desc: String,
    resId: Int,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = "Wallet Icon",
            modifier = Modifier
                .size(44.dp)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = NunchukTheme.typography.body,
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = subtext,
                    style = NunchukTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = NunchukTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondary
            )
        }
    }
}

@Composable
@Preview
fun SubscriptionRequiredBanner(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Color(0xFFFFD54F),
                shape = RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(id = R.string.nc_subscription_required),
            style = NunchukTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
    }
}

@Preview
@Composable
fun WalletTypeItemPreview() {
    WalletTypeItem(
        title = "Personal Wallet",
        desc = "Create a wallet for personal use",
        resId = R.drawable.ic_wallet_type_assisted_wallet
    )
}

@PreviewLightDark
@Composable
fun WalletIntermediaryScreenPreview() {
    WalletIntermediaryScreen()
}

enum class WalletType {
    CUSTOM,
    HOT,
    GROUP,
    DECOY,
    ASSISTED,
    UNASSISTED
}