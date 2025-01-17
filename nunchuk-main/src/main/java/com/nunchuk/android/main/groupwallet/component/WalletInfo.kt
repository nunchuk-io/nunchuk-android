package com.nunchuk.android.main.groupwallet.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.WalletExtendedProvider
import com.nunchuk.android.main.R
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.wallet.util.toReadableString

@Composable
internal fun WalletInfo(
    groupSandbox: GroupSandbox? = null,
    onEditClicked: () -> Unit = {},
    onCopyLinkClicked: () -> Unit = {},
    onShowQRCodeClicked: () -> Unit = {},
) {
    val requireSigns = groupSandbox?.m ?: 0
    val totalSigns = groupSandbox?.n ?: 0
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
                        text = groupSandbox?.name.orEmpty(),
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
                                .background(Color(0xFFF5F5F5))
                        )

                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = groupSandbox?.addressType?.toReadableString(LocalContext.current)
                                ?: "",
                            style = NunchukTheme.typography.bodySmall.copy(
                                color = colorResource(
                                    id = R.color.nc_white_color
                                )
                            )
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 0.5.dp,
                color = colorResource(id = R.color.nc_bg_mid_gray)
            )

            Row(
                modifier = Modifier.padding(top = 12.dp)
                    .clickable { onCopyLinkClicked() },
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
                    modifier = Modifier.padding(start = 20.dp)
                        .clickable { onShowQRCodeClicked() },
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

                Row(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .clickable { onEditClicked() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_setting_2),
                        contentDescription = "Wallet icon",
                        tint = colorResource(id = R.color.nc_white_color)
                    )

                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(id = R.string.nc_settings),
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
        WalletInfo()
    }
}