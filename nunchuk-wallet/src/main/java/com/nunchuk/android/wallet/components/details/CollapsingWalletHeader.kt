package com.nunchuk.android.wallet.components.details

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.wallet.R

internal enum class HeaderTheme {
    DEFAULT,
    FREE_GROUP,
    DEACTIVATED,
    NEED_BACKUP,
    ASSISTED,
}

internal data class WalletHeaderUiModel(
    val title: String,
    val configurationText: String,
    val shareLabel: String?,
    val showShareBadge: Boolean,
    val btcAmount: String,
    val cashAmount: String,
    val spendableLabel: String?,
    val theme: HeaderTheme,
    val isSendEnabled: Boolean,
    val isViewCoinEnabled: Boolean,
    val isFacilitatorAdmin: Boolean,
    val hideWalletDetail: Boolean,
)

@Composable
internal fun rememberWalletHeaderModel(state: WalletDetailsState): WalletHeaderUiModel {
    val wallet: Wallet = state.walletExtended.wallet
    val hide = state.hideWalletDetailLocal
    val theme = when {
        state.isFreeGroupWallet && !state.isDeprecatedGroupWallet && !wallet.archived ->
            HeaderTheme.FREE_GROUP

        state.walletStatus == WalletStatus.REPLACED.name
                || state.walletStatus == WalletStatus.LOCKED.name
                || state.isDeprecatedGroupWallet
                || wallet.archived -> HeaderTheme.DEACTIVATED

        wallet.needBackup -> HeaderTheme.NEED_BACKUP
        state.isAssistedWallet -> HeaderTheme.ASSISTED
        else -> HeaderTheme.DEFAULT
    }

    val shareLabel: String? = when (theme) {
        HeaderTheme.DEACTIVATED -> stringResource(R.string.nc_deactivated)
        HeaderTheme.ASSISTED -> Utils.maskValue(stringResource(R.string.nc_assisted), hide)
        else -> if (state.walletExtended.isShared) stringResource(R.string.nc_text_shared) else null
    }
    val showShareBadge = state.walletExtended.isShared || state.isAssistedWallet ||
            state.walletStatus == WalletStatus.REPLACED.name || state.isDeprecatedGroupWallet

    val configurationText = configurationLabel(wallet, hide)
    val title = if (state.isDeprecatedGroupWallet) {
        stringResource(R.string.nc_deprecated_prefix) + wallet.name
    } else wallet.name

    val spendable = state.noTimelockCoinsAmount.value > 0
    val spendableLabel = if (spendable) {
        stringResource(
            R.string.nc_spendable_now,
            Utils.maskValue(state.noTimelockCoinsAmount.getBTCAmount(), hide)
        )
    } else null

    return WalletHeaderUiModel(
        title = title,
        configurationText = configurationText,
        shareLabel = shareLabel,
        showShareBadge = showShareBadge,
        btcAmount = Utils.maskValue(wallet.getBTCAmount(), hide),
        cashAmount = Utils.maskValue(wallet.getCurrencyAmount(), hide),
        spendableLabel = spendableLabel,
        theme = theme,
        isSendEnabled = state.walletStatus != WalletStatus.LOCKED.name,
        isViewCoinEnabled = state.isHasCoin,
        isFacilitatorAdmin = state.role == AssistedWalletRole.FACILITATOR_ADMIN,
        hideWalletDetail = hide,
    )
}

@Composable
private fun configurationLabel(wallet: Wallet, hide: Boolean): String {
    val total = wallet.signers.size
    val required = wallet.totalRequireSigns
    return when {
        hide -> "•".repeat(6)
        wallet.miniscript.isNotEmpty() -> stringResource(R.string.nc_miniscript)
        total == 0 || required == 0 -> stringResource(R.string.nc_wallet_not_configured)
        total == 1 && required == 1 -> stringResource(R.string.nc_wallet_single_sig)
        else -> "$required/$total ${stringResource(R.string.nc_wallet_multisig)}"
    }
}

/**
 * Holds the collapsing-header animation state and exposes a [NestedScrollConnection]
 * to drive collapse from a [androidx.compose.foundation.lazy.LazyColumn] below.
 */
internal class CollapsingHeaderState(
    val expandedBodyHeightPx: Float,
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set

    val collapseFraction: Float
        get() = if (expandedBodyHeightPx == 0f) 0f
        else (-offsetPx / expandedBodyHeightPx).coerceIn(0f, 1f)

    suspend fun snapToNearest() {
        val target = if (collapseFraction < 0.5f) 0f else -expandedBodyHeightPx
        if (offsetPx == target) return
        animate(
            initialValue = offsetPx,
            targetValue = target,
            animationSpec = tween(durationMillis = 250),
        ) { value, _ -> offsetPx = value }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            val newOffset = (offsetPx + delta).coerceIn(-expandedBodyHeightPx, 0f)
            val consumed = newOffset - offsetPx
            offsetPx = newOffset
            return Offset(0f, consumed)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val fraction = collapseFraction
            if (fraction > 0f && fraction < 1f) {
                snapToNearest()
            }
            return Velocity.Zero
        }
    }
}

@Composable
internal fun rememberCollapsingHeaderState(expandedBodyHeight: Dp): CollapsingHeaderState {
    val px = with(LocalDensity.current) { expandedBodyHeight.toPx() }
    return remember(px) { CollapsingHeaderState(px) }
}

/**
 * Full collapsing wallet header (pinned toolbar + collapsing body + compact strip).
 * Caller is responsible for hosting it inside a column where the LazyColumn below
 * shares the nested-scroll connection from [CollapsingHeaderState].
 */
@Composable
internal fun CollapsingWalletHeader(
    model: WalletHeaderUiModel,
    headerState: CollapsingHeaderState,
    showSearch: Boolean,
    showMenu: Boolean,
    isFreeGroupWallet: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit,
    onToggleMask: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onViewCoin: () -> Unit,
    onWalletConfig: () -> Unit,
    onSpendable: () -> Unit,
) {
    val fraction = headerState.collapseFraction
    val headerBackground: Brush = when (model.theme) {
        HeaderTheme.FREE_GROUP -> Brush.verticalGradient(
            listOf(Color(0xFF2B74A9), Color(0xFF084B7B))
        )

        HeaderTheme.ASSISTED -> Brush.verticalGradient(
            listOf(Color(0xFF1C4A21), Color(0xFF2F766D))
        )

        HeaderTheme.DEACTIVATED -> Brush.verticalGradient(
            listOf(Color(0xFF595959), Color(0xFF595959))
        )

        HeaderTheme.NEED_BACKUP -> Brush.verticalGradient(
            listOf(Color(0xFF8E6F00), Color(0xFF8E6F00))
        )

        else -> Brush.verticalGradient(
            listOf(Color(0xFF031F2B), Color(0xFF2F466C))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        HeaderToolbar(
            title = model.title,
            onBack = onBack,
            onSearch = onSearch,
            onMenu = onMenu,
            showSearch = showSearch,
            showMenu = showMenu,
            isFreeGroupWallet = isFreeGroupWallet,
        )

        // The compact strip appears as we collapse — BTC + inline icons.
        if (fraction > 0.01f) {
            CompactStrip(
                model = model,
                alpha = ((fraction - 0.5f) * 2f).coerceIn(0f, 1f),
                onToggleMask = onToggleMask,
                onSend = onSend,
                onReceive = onReceive,
                onViewCoin = onViewCoin,
            )
        }

        // The expanded body collapses from EXPANDED_BODY_HEIGHT to 0.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(lerp(EXPANDED_BODY_HEIGHT, 0.dp, fraction)),
        ) {
            if (fraction < 0.99f) {
                ExpandedBody(
                    model = model,
                    alpha = (1f - fraction * 2f).coerceIn(0f, 1f),
                    onToggleMask = onToggleMask,
                    onWalletConfig = onWalletConfig,
                    onSpendable = onSpendable,
                    onSend = onSend,
                    onReceive = onReceive,
                    onViewCoin = onViewCoin,
                )
            }
        }
    }
}

@Composable
private fun HeaderToolbar(
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit,
    showSearch: Boolean,
    showMenu: Boolean,
    isFreeGroupWallet: Boolean,
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
                        painter = painterResource(
                            if (isFreeGroupWallet) R.drawable.ic_groups_menu
                            else R.drawable.ic_search_white
                        ),
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactStrip(
    model: WalletHeaderUiModel,
    alpha: Float,
    onToggleMask: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onViewCoin: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model.btcAmount,
            style = NunchukTheme.typography.title.copy(color = Color.White, fontSize = 16.sp),
        )
        Spacer(Modifier.size(8.dp))
        MaskToggleIcon(hide = model.hideWalletDetail, onClick = onToggleMask)
        Spacer(Modifier.weight(1f))
        CompactActionIcon(
            icon = R.drawable.ic_sending_bitcoin,
            enabled = model.isSendEnabled,
            onClick = onSend,
        )
        Spacer(Modifier.size(8.dp))
        CompactActionIcon(
            icon = R.drawable.ic_receive_bitcoin,
            enabled = true,
            onClick = onReceive,
        )
        Spacer(Modifier.size(8.dp))
        CompactActionIcon(
            icon = R.drawable.ic_bitcoin_dark,
            enabled = model.isViewCoinEnabled && !model.isFacilitatorAdmin,
            onClick = onViewCoin,
        )
    }
}

@Composable
private fun ExpandedBody(
    model: WalletHeaderUiModel,
    alpha: Float,
    onToggleMask: () -> Unit,
    onWalletConfig: () -> Unit,
    onSpendable: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onViewCoin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConfigurationBadgeRow(model = model)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = model.btcAmount,
                style = NunchukTheme.typography.heading.copy(color = Color.White),
            )
            Spacer(Modifier.size(8.dp))
            MaskToggleIcon(hide = model.hideWalletDetail, onClick = onToggleMask)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = model.cashAmount,
            style = NunchukTheme.typography.body.copy(color = cashAmountColor(model.theme)),
        )
        if (model.spendableLabel != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onSpendable() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = model.spendableLabel,
                    style = NunchukTheme.typography.bodySmall.copy(color = Color.White),
                )
                Spacer(Modifier.size(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier
                .clickable { onWalletConfig() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            text = stringResource(R.string.nc_wallet_view_wallet_config),
            style = NunchukTheme.typography.body.copy(
                color = Color.White,
                textDecoration = TextDecoration.Underline,
            ),
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BigActionButton(
                icon = R.drawable.ic_sending_bitcoin,
                label = stringResource(R.string.nc_wallet_send),
                enabled = model.isSendEnabled,
                onClick = onSend,
            )
            BigActionButton(
                icon = R.drawable.ic_receive_bitcoin,
                label = stringResource(R.string.nc_wallet_receive),
                enabled = true,
                onClick = onReceive,
            )
            BigActionButton(
                icon = R.drawable.ic_bitcoin_dark,
                label = stringResource(R.string.nc_view_coins),
                enabled = model.isViewCoinEnabled && !model.isFacilitatorAdmin,
                onClick = onViewCoin,
            )
        }
    }
}

@Composable
private fun ConfigurationBadgeRow(model: WalletHeaderUiModel) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFFEAEAEA), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = model.configurationText,
                style = NunchukTheme.typography.captionTitle.copy(color = Color(0xFF031F2B)),
            )
        }
        if (model.showShareBadge && model.shareLabel != null) {
            Spacer(Modifier.size(4.dp))
            Row(
                modifier = Modifier
                    .background(Color(0xFFEAEAEA), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.theme != HeaderTheme.DEACTIVATED) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wallet_small),
                        contentDescription = null,
                        tint = Color(0xFF031F2B),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                }
                Text(
                    text = model.shareLabel,
                    style = NunchukTheme.typography.captionTitle.copy(color = Color(0xFF031F2B)),
                )
            }
        }
    }
}

@Composable
private fun BigActionButton(
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
private fun CompactActionIcon(
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
private fun MaskToggleIcon(hide: Boolean, onClick: () -> Unit) {
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

private fun cashAmountColor(theme: HeaderTheme): Color = when (theme) {
    HeaderTheme.FREE_GROUP -> Color.White
    HeaderTheme.NEED_BACKUP -> Color(0xFFFFE7A1)
    else -> Color(0xFFB5DCFA)
}

internal val EXPANDED_BODY_HEIGHT = 280.dp
internal val TOOLBAR_HEIGHT = 56.dp
