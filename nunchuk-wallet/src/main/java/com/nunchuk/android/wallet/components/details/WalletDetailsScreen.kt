package com.nunchuk.android.wallet.components.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.asFlow
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.model.BannerState
import com.nunchuk.android.model.LiquidNetworkStatus
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.type.WalletType

@Composable
internal fun WalletDetailsScreen(
    viewModel: WalletDetailsViewModel,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit,
    onToggleMask: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onViewCoin: () -> Unit,
    onWalletConfig: () -> Unit,
    onSpendable: () -> Unit,
    onTransactionClick: (com.nunchuk.android.model.Transaction) -> Unit,
    onClaimInheritance: () -> Unit,
    onNeedBackup: () -> Unit,
    onBannerBackupAndRegister: () -> Unit,
    onBannerBackupOnly: () -> Unit,
    onBannerRegisterOnly: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
    onCopyAddress: (String) -> Unit,
    onShareAddress: (String) -> Unit,
    onAcceptOrDenyReplaceGroup: (String, Boolean) -> Unit,
    onOpenReplacementSetup: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenChat: () -> Unit,
) {
    val state by viewModel.state.asFlow().collectAsStateWithLifecycle(WalletDetailsState())
    val extendedTransactions by viewModel.extendedTransactions.collectAsState()

    val isStableWallet = state.walletExtended.wallet.walletType == WalletType.LIQUID
    val expandedBodyHeight = if (isStableWallet) STABLE_EXPANDED_BODY_HEIGHT else EXPANDED_BODY_HEIGHT
    val headerState = rememberCollapsingHeaderState(expandedBodyHeight = expandedBodyHeight)
    val headerModel = rememberWalletHeaderModel(state)
    val stableHeaderModel = rememberStableWalletHeaderModel(state)
    val listState = rememberLazyListState()

    val isInactiveAssisted = viewModel.isInactiveAssistedWallet()
    val needsBackup = state.walletExtended.wallet.needBackup ||
            (state.isNeedBackUpGroupWallet && state.isFreeGroupWallet && !state.isDeprecatedGroupWallet)
    val warning: WalletWarning? = when {
        state.isClaimWallet && state.walletExtended.wallet.balance.value > 0 ->
            WalletWarning.ClaimInheritance

        needsBackup -> WalletWarning.NeedBackup(state.isFreeGroupWallet)
        state.bannerState != null -> WalletWarning.Banner(state.bannerState as BannerState)
        isInactiveAssisted -> WalletWarning.InactiveAssisted
        else -> null
    }

    val isFacilitatorAdmin = state.role == AssistedWalletRole.FACILITATOR_ADMIN
    val isWalletNamed = state.walletExtended.wallet.name.isNotEmpty()
    val showSearch = isWalletNamed && !state.isFreeGroupWallet
    val showMenu = state.walletStatus != com.nunchuk.android.model.wallet.WalletStatus.LOCKED.name &&
            !isFacilitatorAdmin && state.hasTransactions || state.isFreeGroupWallet
    val showChat = state.isFreeGroupWallet && !state.hideWalletDetailLocal

    // Drive chat bar auto-collapse from list scroll direction.
    LaunchedEffect(listState, state.isFreeGroupWallet) {
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val scrollingDown = index > lastIndex ||
                        (index == lastIndex && offset > lastOffset)
                val scrollingUp = index < lastIndex ||
                        (index == lastIndex && offset < lastOffset)
                val current = viewModel.getChatBarState()
                when {
                    scrollingDown && current == ChatBarState.EXPANDED ->
                        viewModel.setChatBarState(ChatBarState.AUTO_COLLAPSED)

                    scrollingUp && current == ChatBarState.AUTO_COLLAPSED ->
                        viewModel.setChatBarState(ChatBarState.EXPANDED)
                }
                lastIndex = index
                lastOffset = offset
            }
    }

    NunchukTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(headerState.nestedScrollConnection),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isStableWallet) {
                    StableCollapsingWalletHeader(
                        model = stableHeaderModel,
                        headerState = headerState,
                        showSearch = showSearch,
                        showMenu = showMenu,
                        onBack = onBack,
                        onSearch = onSearch,
                        onMenu = onMenu,
                        onToggleMask = onToggleMask,
                        onSend = onSend,
                        onReceive = onReceive,
                    )
                } else {
                    CollapsingWalletHeader(
                        model = headerModel,
                        headerState = headerState,
                        showSearch = showSearch,
                        showMenu = showMenu,
                        isFreeGroupWallet = state.isFreeGroupWallet,
                        onBack = onBack,
                        onSearch = onSearch,
                        onMenu = onMenu,
                        onToggleMask = onToggleMask,
                        onSend = onSend,
                        onReceive = onReceive,
                        onViewCoin = onViewCoin,
                        onWalletConfig = onWalletConfig,
                        onSpendable = onSpendable,
                    )
                }

                if (warning != null) {
                    WalletWarningBanner(
                        warning = warning,
                        onClaimInheritance = onClaimInheritance,
                        onNeedBackup = onNeedBackup,
                        onBannerBackupAndRegister = onBannerBackupAndRegister,
                        onBannerBackupOnly = onBannerBackupOnly,
                        onBannerRegisterOnly = onBannerRegisterOnly,
                        onOpenExternalLink = onOpenExternalLink,
                    )
                }

                if (isStableWallet && state.liquidNetworkStatus.hasIssue) {
                    NcHintMessage(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        messages = listOf(ClickAbleText(state.liquidNetworkStatus.viewMessage)),
                        type = when (state.liquidNetworkStatus.severity) {
                            LiquidNetworkStatus.Severity.ERROR -> HighlightMessageType.ERROR
                            else -> HighlightMessageType.WARNING
                        },
                    )
                }

                if (state.replaceGroups.isNotEmpty() && !state.isDeprecatedGroupWallet) {
                    ReplacedGroupView(
                        replacedGroups = state.replaceGroups,
                        onAcceptOrDeny = onAcceptOrDenyReplaceGroup,
                        onOpenSetupGroupWallet = onOpenReplacementSetup,
                    )
                }

                TimelockWarningBanner(
                    nearestTimeLock = state.nearestTimeLock,
                    currentBlock = state.currentBlock,
                    onViewCoins = onViewCoin,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when {
                        !state.transactionsLoaded -> Unit
                        state.hasTransactions -> TransactionList(
                            items = extendedTransactions,
                            hideWalletDetail = state.hideWalletDetailLocal,
                            listState = listState,
                            isStableWallet = isStableWallet,
                            usdtAssetId = state.usdtAssetId,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = if (showChat) 88.dp else 56.dp,
                            ),
                            onClick = onTransactionClick,
                        )

                        isFacilitatorAdmin -> EmptyTransactionFacilitatorAdminView()
                        else -> EmptyTransactionView(
                            address = state.unusedAddress,
                            onCopyAddress = onCopyAddress,
                            onShareAddress = onShareAddress,
                        )
                    }
                }
            }

            if (showChat) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter),
                ) {
                    GroupWalletChatView(
                        viewModel = viewModel,
                        chatBarState = state.chatBarState,
                        messages = state.groupChatMessages,
                        unreadCount = state.unreadMessagesCount,
                        onSendMessage = onSendMessage,
                        onOpenChat = onOpenChat,
                    )
                }
            }
        }
    }
}
