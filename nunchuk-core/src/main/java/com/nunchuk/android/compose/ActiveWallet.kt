package com.nunchuk.android.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.utils.Utils

@Composable
fun ActiveWallet(
    walletsExtended: WalletExtended,
    hideWalletDetail: Boolean,
    isAssistedWallet: Boolean,
    role: String = AssistedWalletRole.NONE.name,
    useLargeFont: Boolean = false,
    walletStatus: String = ""
) {
    val wallet = walletsExtended.wallet
    val balance = "(${wallet.getCurrencyAmount()})"
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = wallet.name, style = NunchukTheme.typography.title, color = Color.White
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = Utils.maskValue(
                    wallet.getBTCAmount(),
                    role == AssistedWalletRole.KEYHOLDER_LIMITED.name || hideWalletDetail
                ),
                style = if (useLargeFont) NunchukTheme.typography.title else NunchukTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = Utils.maskValue(
                    balance, role == AssistedWalletRole.KEYHOLDER_LIMITED.name || hideWalletDetail
                ),
                style = if (useLargeFont) NunchukTheme.typography.body else NunchukTheme.typography.bodySmall,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            if (walletsExtended.isShared || isAssistedWallet || walletStatus == WalletStatus.REPLACED.name) {
                Badge(containerColor = Color.White) {
                    if (walletStatus != WalletStatus.REPLACED.name) {
                        Icon(
                            modifier = Modifier.padding(start = 8.dp),
                            painter = painterResource(id = R.drawable.ic_wallet_small),
                            contentDescription = "Wallet"
                        )
                    }
                    val walletTypeName = if (walletStatus == WalletStatus.REPLACED.name) {
                        stringResource(R.string.nc_deactivated)
                    } else if (isAssistedWallet) {
                        Utils.maskValue(
                            stringResource(R.string.nc_assisted), hideWalletDetail
                        )
                    } else {
                        Utils.maskValue(
                            stringResource(R.string.nc_text_shared), hideWalletDetail
                        )
                    }
                    Text(
                        modifier = Modifier.padding(
                            start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp
                        ),
                        text = walletTypeName,
                        style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                    )
                }
            }

            Badge(modifier = Modifier.padding(top = 4.dp), containerColor = Color.White) {
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
                        horizontal = 8.dp, vertical = 2.dp
                    ),
                    text = text,
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                )
            }
        }
    }
}

@Composable
fun getWalletColors(
    walletsExtended: WalletExtended?,
    isAssistedWallet: Boolean,
    group: ByzantineGroup?,
    role: String,
    isLocked: Boolean,
    inviterName: String,
    walletStatus: String
): List<Color> {
    val colors = if (inviterName.isNotEmpty() || walletsExtended == null) {
        listOf(MaterialTheme.colorScheme.yellowishOrange, MaterialTheme.colorScheme.yellowishOrange)
    } else if ((group != null && role == AssistedWalletRole.KEYHOLDER_LIMITED.name) || isLocked
        || walletStatus == WalletStatus.LOCKED.name
        || walletStatus == WalletStatus.REPLACED.name
    ) {
        listOf(NcColor.greyDark, NcColor.greyDark)
    } else if (group != null || isAssistedWallet) {
        listOf(MaterialTheme.colorScheme.ming, MaterialTheme.colorScheme.everglade)
    } else if (walletsExtended.wallet.needBackup) {
        listOf(
            colorResource(id = R.color.nc_beeswax_dark),
            colorResource(id = R.color.nc_beeswax_dark)
        )
    } else {
        listOf(
            colorResource(id = R.color.nc_primary_light_color),
            colorResource(id = R.color.nc_primary_color)
        )
    }
    return colors
}