package com.nunchuk.android.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.nunchuk.android.core.util.MAX_FRACTION_DIGITS
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getDisplayCurrency
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isFacilitatorAdmin
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.Utils

@Composable
fun ActiveWallet(
    walletsExtended: WalletExtended,
    hideWalletDetail: Boolean,
    isAssistedWallet: Boolean,
    role: String = AssistedWalletRole.NONE.name,
    useLargeFont: Boolean = false,
    walletStatus: String = "",
    isSandboxWallet: Boolean = false,
    isDeprecatedGroupWallet: Boolean = false,
) {
    val wallet = walletsExtended.wallet
    val isLiquid = wallet.walletType == WalletType.LIQUID
    val mask = role.toRole.isKeyHolderLimited || role.toRole.isFacilitatorAdmin || hideWalletDetail
    val name = if (isDeprecatedGroupWallet) "[DEPRECATED] ${wallet.name}" else wallet.name

    val primaryAmount = if (isLiquid) "${wallet.usdtBalance.formatUsdtToken()} USDT" else wallet.getBTCAmount()
    val cashAmount = if (isLiquid) "(${wallet.usdtBalance.formatUsdtAsCash()})" else "(${wallet.getCurrencyAmount()})"

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name, style = NunchukTheme.typography.title, color = Color.White
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = Utils.maskValue(primaryAmount, mask),
                style = if (useLargeFont) NunchukTheme.typography.title else NunchukTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = Utils.maskValue(cashAmount, mask),
                style = if (useLargeFont) NunchukTheme.typography.body else NunchukTheme.typography.bodySmall,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            if (isLiquid) {
                Badge(containerColor = Color.White) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 8.dp, vertical = 2.dp
                        ),
                        text = stringResource(R.string.nc_liquid),
                        style = NunchukTheme.typography.titleSmall.copy(
                            fontSize = 10.sp,
                            color = colorResource(R.color.nc_grey_g7)
                        )
                    )
                }
            } else if (walletsExtended.isShared || isAssistedWallet
                || walletStatus == WalletStatus.REPLACED.name
                || walletStatus == WalletStatus.LOCKED.name
                || isSandboxWallet
                || isDeprecatedGroupWallet
            ) {
                Badge(containerColor = Color.White) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isSandboxWallet && !isDeprecatedGroupWallet) {
                        Icon(
                            modifier = Modifier.padding(end = 4.dp),
                            painter = painterResource(id = R.drawable.ic_circle_three),
                            contentDescription = "Wallet",
                            tint = colorResource(R.color.nc_grey_g7)
                        )
                    } else if (walletStatus != WalletStatus.REPLACED.name && !isDeprecatedGroupWallet) {
                        Icon(
                            modifier = Modifier.padding(end = 4.dp),
                            painter = painterResource(id = R.drawable.ic_wallet_small),
                            contentDescription = "Wallet",
                            tint = colorResource(R.color.nc_grey_g7)
                        )
                    }
                    val walletTypeName =
                        if (walletStatus == WalletStatus.REPLACED.name || isDeprecatedGroupWallet) {
                            stringResource(R.string.nc_deactivated)
                        } else if (isAssistedWallet || walletStatus == WalletStatus.LOCKED.name) {
                            Utils.maskValue(
                                stringResource(R.string.nc_assisted), hideWalletDetail
                            )
                        } else if (isSandboxWallet) {
                            Utils.maskValue(
                                stringResource(R.string.nc_shared), hideWalletDetail
                            )
                        } else {
                            Utils.maskValue(
                                stringResource(R.string.nc_text_shared), hideWalletDetail
                            )
                        }
                    Text(
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                        text = walletTypeName,
                        style = NunchukTheme.typography.titleSmall.copy(
                            fontSize = 10.sp,
                            color = colorResource(R.color.nc_grey_g7)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            if (!isLiquid) {
                Badge(modifier = Modifier.padding(top = 4.dp), containerColor = Color.White) {
                    val requireSigns = wallet.totalRequireSigns
                    val totalSigns = wallet.signers.size
                    val text = if (hideWalletDetail) {
                        '\u2022'.toString().repeat(6)
                    } else if (wallet.miniscript.isNotEmpty()) {
                        stringResource(R.string.nc_miniscript)
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
                        style = NunchukTheme.typography.titleSmall.copy(
                            fontSize = 10.sp,
                            color = colorResource(R.color.nc_grey_g7)
                        )
                    )
                }
            }
        }
    }
}

private fun Amount.formatUsdtToken(): String =
    pureBTC().formatDecimalWithoutZero(maxFractionDigits = MAX_FRACTION_DIGITS)

private fun Amount.formatUsdtAsCash(): String =
    "${getDisplayCurrency()}${pureBTC().formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)}"

fun isLimitAccess(group: ByzantineGroup?, role: String, walletStatus: String?): Boolean {
    return group?.isLocked == true
            || (group != null && role == AssistedWalletRole.KEYHOLDER_LIMITED.name)
            || walletStatus == WalletStatus.LOCKED.name
            || walletStatus == WalletStatus.REPLACED.name
}

@Composable
fun getWalletColors(
    isJoined: Boolean = true,
    wallet: Wallet?,
    hasGroup: Boolean,
    isAssistedWallet: Boolean,
    isLimitAccess: Boolean,
    isFreeGroupWallet: Boolean
): List<Color> {
    val colors = if (!isJoined || wallet == null) {
        listOf(MaterialTheme.colorScheme.fillBeewax, MaterialTheme.colorScheme.fillBeewax)
    } else if (isLimitAccess || wallet.archived) {
        disableWalletColor
    } else if (wallet.walletType == WalletType.LIQUID) {
        listOf(Color(0xFF189EC9), Color(0xFF0E6A86))
    } else if (hasGroup || isAssistedWallet) {
        listOf(MaterialTheme.colorScheme.ming, MaterialTheme.colorScheme.everglade)
    } else if (isFreeGroupWallet) {
        listOf(
            colorResource(id = R.color.cl_084B7B),
            colorResource(id = R.color.cl_2B74A9)
        )
    } else if (wallet.needBackup) {
        listOf(
            colorResource(id = R.color.nc_beeswax_dark),
            colorResource(id = R.color.nc_beeswax_dark)
        )
    } else {
        listOf(
            colorResource(id = R.color.nc_primary_light_color),
            colorResource(id = R.color.cl_031F2B)
        )
    }
    return colors
}

val disableWalletColor = listOf(NcColor.greyDark, NcColor.greyDark)