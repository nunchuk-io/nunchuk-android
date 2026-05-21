package com.nunchuk.android.wallet.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.MAX_FRACTION_DIGITS
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.getDisplayCurrency
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.R as WidgetR

internal data class StableWalletHeaderUiModel(
    val title: String,
    val primaryAssetTitle: String,
    val primaryAssetAmount: String,
    val primaryAssetCashAmount: String,
    val secondaryAssetTitle: String,
    val secondaryAssetAmount: String,
    val secondaryAssetCashAmount: String,
    val hideWalletDetail: Boolean,
    val isSendEnabled: Boolean,
)

@Composable
internal fun rememberStableWalletHeaderModel(state: WalletDetailsState): StableWalletHeaderUiModel {
    val wallet = state.walletExtended.wallet
    val hide = state.hideWalletDetailLocal

    val usdtRaw = "${wallet.usdtBalance.formatUsdt()} USDT"
    val usdtCashRaw = "(${wallet.usdtBalance.formatUsdtAsUsd()})"
    val lbtcRaw = "${wallet.lbtcBalance.formatLbtc()} LBTC"
    val lbtcCashRaw = wallet.lbtcBalance.formatLbtcAsUsd()

    return StableWalletHeaderUiModel(
        title = wallet.name,
        primaryAssetTitle = "Tether USD",
        primaryAssetAmount = Utils.maskValue(usdtRaw, hide),
        primaryAssetCashAmount = Utils.maskValue(usdtCashRaw, hide),
        secondaryAssetTitle = "Liquid BTC:",
        secondaryAssetAmount = Utils.maskValue(lbtcRaw, hide),
        secondaryAssetCashAmount = Utils.maskValue(lbtcCashRaw, hide),
        hideWalletDetail = hide,
        isSendEnabled = state.walletStatus != WalletStatus.LOCKED.name,
    )
}

// USDT on Liquid uses 8-decimal precision; show up to 8 fractional digits and strip
// trailing zeros so testnet fractional balances aren't rounded down to "0".
private fun Amount.formatUsdt(): String =
    pureBTC().formatDecimalWithoutZero(maxFractionDigits = MAX_FRACTION_DIGITS)

private fun Amount.formatLbtc(): String =
    pureBTC().formatDecimal(minFractionDigits = MAX_FRACTION_DIGITS)

// USDT is pegged 1:1 to USD; display fiat value directly from the token amount.
private fun Amount.formatUsdtAsUsd(): String =
    "${getDisplayCurrency()}${pureBTC().formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)}"

private fun Amount.formatLbtcAsUsd(): String =
    "${getDisplayCurrency()}${pureBTC().fromBTCToCurrency().formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)}"

@Composable
internal fun StableCollapsingWalletHeader(
    model: StableWalletHeaderUiModel,
    headerState: CollapsingHeaderState,
    showSearch: Boolean,
    showMenu: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit,
    onToggleMask: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
) {
    val fraction = headerState.collapseFraction
    val headerBackground: Brush = Brush.verticalGradient(
        listOf(Color(0xFF189EC9), Color(0xFF0E6A86))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        StableHeaderToolbar(
            title = model.title,
            onBack = onBack,
            onSearch = onSearch,
            onMenu = onMenu,
            showSearch = showSearch,
            showMenu = showMenu,
        )

        if (fraction > 0.01f) {
            StableCompactStrip(
                model = model,
                alpha = ((fraction - 0.5f) * 2f).coerceIn(0f, 1f),
                onSend = onSend,
                onReceive = onReceive,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(lerp(STABLE_EXPANDED_BODY_HEIGHT, 0.dp, fraction)),
        ) {
            if (fraction < 0.99f) {
                StableExpandedBody(
                    model = model,
                    alpha = (1f - fraction * 2f).coerceIn(0f, 1f),
                    onToggleMask = onToggleMask,
                    onSend = onSend,
                    onReceive = onReceive,
                )
            }
        }
    }
}

@Composable
private fun StableHeaderToolbar(
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit,
    showSearch: Boolean,
    showMenu: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOOLBAR_HEIGHT),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_back_white),
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            if (showSearch) {
                IconButton(onClick = onSearch) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_white),
                        contentDescription = "Search",
                        tint = Color.White,
                    )
                }
            }
            if (showMenu) {
                IconButton(onClick = onMenu) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_horizontal),
                        contentDescription = "More",
                        tint = Color.White,
                    )
                }
            }
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            text = title,
            style = NunchukTheme.typography.titleLarge.copy(color = Color.White),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun StableExpandedBody(
    model: StableWalletHeaderUiModel,
    alpha: Float,
    onToggleMask: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PrimaryAssetBlock(
                title = model.primaryAssetTitle,
                amount = model.primaryAssetAmount,
                cashAmount = model.primaryAssetCashAmount,
                hideWalletDetail = model.hideWalletDetail,
                onToggleMask = onToggleMask,
            )
        }
        SecondaryAssetRow(
            title = model.secondaryAssetTitle,
            amount = model.secondaryAssetAmount,
            cashAmount = model.secondaryAssetCashAmount,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                24.dp,
                Alignment.CenterHorizontally,
            ),
        ) {
            StableActionButton(
                icon = R.drawable.ic_sending_bitcoin,
                label = stringResource(R.string.nc_wallet_send),
                enabled = model.isSendEnabled,
                onClick = onSend,
            )
            StableActionButton(
                icon = R.drawable.ic_receive_bitcoin,
                label = stringResource(R.string.nc_wallet_receive),
                enabled = true,
                onClick = onReceive,
            )
        }
    }
}

@Composable
private fun PrimaryAssetBlock(
    title: String,
    amount: String,
    cashAmount: String,
    hideWalletDetail: Boolean,
    onToggleMask: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(WidgetR.drawable.ic_usdt),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified,
            )
            Text(
                text = title,
                style = NunchukTheme.typography.bodySmall.copy(color = Color(0xFFEAEAEA)),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = amount,
                style = NunchukTheme.typography.heading.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                ),
            )
            StableMaskToggleIcon(hide = hideWalletDetail, onClick = onToggleMask)
        }
        Text(
            text = cashAmount,
            style = NunchukTheme.typography.bodySmall.copy(color = Color(0xFFEAEAEA)),
        )
    }
}

@Composable
private fun SecondaryAssetRow(
    title: String,
    amount: String,
    cashAmount: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(WidgetR.drawable.ic_liquid_btc),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Unspecified,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = title,
            style = NunchukTheme.typography.bodySmall.copy(color = Color(0xFFEAEAEA)),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = amount,
            style = NunchukTheme.typography.bodySmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = cashAmount,
            style = NunchukTheme.typography.bodySmall.copy(color = Color(0xFFEAEAEA)),
        )
    }
}

@Composable
private fun StableActionButton(
    icon: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5))
                .alpha(if (enabled) 1f else 0.7f)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = NunchukTheme.typography.titleSmall.copy(color = Color.White),
        )
    }
}

@Composable
private fun StableCompactStrip(
    model: StableWalletHeaderUiModel,
    alpha: Float,
    onSend: () -> Unit,
    onReceive: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CompactAssetLine(
                amount = model.primaryAssetAmount,
                cashAmount = model.primaryAssetCashAmount,
            )
            Spacer(Modifier.height(2.dp))
            CompactAssetLine(
                amount = model.secondaryAssetAmount,
                cashAmount = model.secondaryAssetCashAmount,
            )
        }
        Spacer(Modifier.size(12.dp))
        StableCompactActionIcon(
            icon = R.drawable.ic_sending_bitcoin,
            enabled = model.isSendEnabled,
            onClick = onSend,
        )
        Spacer(Modifier.size(8.dp))
        StableCompactActionIcon(
            icon = R.drawable.ic_receive_bitcoin,
            enabled = true,
            onClick = onReceive,
        )
    }
}

@Composable
private fun CompactAssetLine(amount: String, cashAmount: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = amount,
            style = NunchukTheme.typography.title.copy(color = Color.White, fontSize = 14.sp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = cashAmount,
            style = NunchukTheme.typography.bodySmall.copy(color = Color(0xFFEAEAEA)),
        )
    }
}

@Composable
private fun StableCompactActionIcon(
    icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFFF5F5F5))
            .alpha(if (enabled) 1f else 0.7f)
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Unspecified,
        )
    }
}

@Composable
private fun StableMaskToggleIcon(hide: Boolean, onClick: () -> Unit) {
    Icon(
        painter = painterResource(
            if (hide) R.drawable.ic_visibility else R.drawable.ic_hide_pass
        ),
        contentDescription = "Toggle mask",
        tint = Color.White,
        modifier = Modifier
            .size(24.dp)
            .clickable { onClick() },
    )
}

internal val STABLE_EXPANDED_BODY_HEIGHT = 280.dp

@PreviewLightDark
@Composable
private fun StableCollapsingWalletHeaderPreview() {
    NunchukTheme {
        val headerState = remember { CollapsingHeaderState(expandedBodyHeightPx = 1000f) }
        StableCollapsingWalletHeader(
            model = StableWalletHeaderUiModel(
                title = "USDT wallet",
                primaryAssetTitle = "Tether USD",
                primaryAssetAmount = "1024 USDT",
                primaryAssetCashAmount = "($1024.25)",
                secondaryAssetTitle = "Liquid BTC:",
                secondaryAssetAmount = "0.0004 LBTC",
                secondaryAssetCashAmount = "($29.80)",
                hideWalletDetail = false,
                isSendEnabled = true,
            ),
            headerState = headerState,
            showSearch = true,
            showMenu = true,
            onBack = {},
            onSearch = {},
            onMenu = {},
            onToggleMask = {},
            onSend = {},
            onReceive = {},
        )
    }
}
