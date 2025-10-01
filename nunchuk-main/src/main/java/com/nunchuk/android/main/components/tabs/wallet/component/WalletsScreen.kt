package com.nunchuk.android.main.components.tabs.wallet.component

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.core.util.BTC_CURRENCY_EXCHANGE_RATE
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.GroupWalletUi
import com.nunchuk.android.main.components.tabs.wallet.LoadingIndicator
import com.nunchuk.android.main.components.tabs.wallet.WalletsState
import com.nunchuk.android.main.components.tabs.wallet.emptystate.WalletEmptyStateView
import com.nunchuk.android.main.components.tabs.wallet.totalbalance.TotalBalanceView
import com.nunchuk.android.main.groupwallet.FreeGroupWalletActivity
import com.nunchuk.android.main.membership.byzantine.views.PendingWalletView
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.nav.NunchukNavigator
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun WalletsScreen(
    activity: Activity,
    navigator: NunchukNavigator,
    hasSigner: Boolean,
    state: WalletsState,
    onNfcClick: () -> Unit,
    onCampaignClick: () -> Unit,
    onIntroContainerClick: (stage: MembershipStage) -> Unit,
    onBannerClick: (Banner) -> Unit,
    onAccept: (GroupWalletUi) -> Unit,
    denyInviteMember: (String) -> Unit,
    showWalletReplacedDialog: (oldWalletId: String, replaceByWalletId: String) -> Unit,
    openWalletDetailsScreen: (String) -> Unit,
    getWalletDetail: (String) -> Unit,
    openArchivedWalletsScreen: () -> Unit,
    onMove: (from: String, to: String) -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val snackState = remember { SnackbarHostState() }
    val banner = state.banner
    val isShowEmptyState = state.groupWalletUis.isEmpty()
    val isAssistedBannerGone = state.stage == MembershipStage.DONE || state.allGroups.isNotEmpty()
    val groupWalletUis = state.groupWalletUis
    val useLargeFont = state.homeDisplaySetting.useLargeFont
    val hideWalletDetail = state.walletSecuritySetting.hideWalletDetail
    val assistedWallets = state.assistedWallets.associateBy { it.localId }
    val deprecatedGroupWalletIds = state.deprecatedGroupWalletIds
    var denyInviteMemberGroupId by rememberSaveable { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(from.key as String, to.key as String)
        ViewCompat.performHapticFeedback(
            view,
            HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
        )
    }
    val totalBalance = state.groupWalletUis
        .filter { !it.role.toRole.isKeyHolderLimited }
        .sumOf { it.wallet?.wallet?.balance?.value ?: 0 }
    val totalInCurrency = Amount(value = totalBalance).getCurrencyAmount()
    val totalInBtc = Amount(value = totalBalance).getBTCAmount()

    var isTotalBalanceVisible by remember { mutableStateOf(true) }
    var isUserScrolling by remember { mutableStateOf(false) }

    if (state.wallets.isNotEmpty() && state.homeDisplaySetting.showTotalBalance) {
        LaunchedEffect(lazyListState.interactionSource) {
            lazyListState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> isUserScrolling = true
                    is DragInteraction.Stop, is DragInteraction.Cancel -> isUserScrolling = false
                }
            }
        }

        LaunchedEffect(lazyListState) {
            var previousIndex = lazyListState.firstVisibleItemIndex
            var previousOffset = lazyListState.firstVisibleItemScrollOffset
            snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
                .collect { (currentIndex, currentOffset) ->
                    if (isUserScrolling) {
                        if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousOffset)) {
                            isTotalBalanceVisible = false // Scrolling down
                        } else if (currentIndex < previousIndex || (currentOffset < previousOffset)) {
                            isTotalBalanceVisible = true // Scrolling up
                        }
                    }
                    previousIndex = currentIndex
                    previousOffset = currentOffset
                }
        }
    }

    NcScaffold(
        topBar = {
            WalletsTopBar(
                campaign = state.campaign,
                isHasWallet = state.wallets.isNotEmpty(),
                localReferrerCode = state.localReferrerCode,
                onNfcClick = onNfcClick,
                onCampaignClick = onCampaignClick
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.wallets.isNotEmpty() && state.homeDisplaySetting.showTotalBalance && isTotalBalanceVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                TotalBalanceView(
                    isLargeFont = state.homeDisplaySetting.useLargeFont,
                    balanceSatoshis = totalInBtc,
                    balanceFiat = totalInCurrency,
                    btcPrice = BTC_CURRENCY_EXCHANGE_RATE,
                    isHideBalance = state.walletSecuritySetting.hideWalletDetail,
                    exchangeRateUnit = state.homeDisplaySetting.exchangeRateUnit
                )
            }
        },
        snackState = snackState
    ) { padding ->
        if (state.isWalletLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            if (isShowEmptyState) {
                WalletEmptyStateView(
                    modifier = Modifier.padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    activityContext = activity,
                    navigator = navigator,
                    groupStage = state.stage,
                    assistedWalletId = state.assistedWallets.firstOrNull()?.localId.orEmpty(),
                    hasSigner = hasSigner,
                    state = state,
                    openArchivedWalletsScreen = openArchivedWalletsScreen
                )
            } else {
                Column(
                    modifier = Modifier
                        .padding(top = padding.calculateTopPadding())
                        .fillMaxSize()
                ) {
                    if (banner != null && state.plans?.isEmpty() == true) {
                        NonSubscriberBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            banner = banner, onClick = onBannerClick
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp),
                        state = lazyListState
                    ) {
                        if (!isAssistedBannerGone) {
                            item {
                                AssistedWalletIntro(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    state = state,
                                    stage = state.stage,
                                    context = context,
                                    onClick = { onIntroContainerClick(state.stage) }
                                )
                            }
                        }

                        item {
                            WalletsHeader(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                status = state.connectionStatus,
                                chain = state.chain,
                                onAddWalletClick = {
                                    navigator.openWalletIntermediaryScreen(
                                        activity,
                                        state.isLeaveRoom
                                    )
                                }
                            )
                        }

                        if (state.groupWalletUis.isEmpty() && state.totalArchivedWallet == 0) {
                            item {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    text = stringResource(R.string.nc_text_you_have_not_add_wallet),
                                    textAlign = TextAlign.Center,
                                    style = NunchukTheme.typography.body,
                                )
                            }
                        } else {
                            items(state.groupWalletUis, key = { it.id }) {
                                val briefWallet = assistedWallets[it.wallet?.wallet?.id.orEmpty()]
                                ReorderableItem(
                                    modifier = Modifier
                                        .padding(top = 12.dp),
                                    state = reorderableLazyListState,
                                    key = it.id,
                                    enabled = !it.wallet?.wallet?.id.isNullOrEmpty(),
                                ) { isDragging ->
                                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                                    Surface(shadowElevation = elevation) {
                                        PendingWalletView(
                                            modifier = Modifier
                                                .longPressDraggableHandle(enabled = !it.wallet?.wallet?.id.isNullOrEmpty()),
                                            group = it.group,
                                            sandbox = it.sandbox,
                                            isSandboxWallet = it.isSandboxWallet,
                                            walletsExtended = it.wallet,
                                            inviterName = it.inviterName,
                                            isAssistedWallet = briefWallet?.status == WalletStatus.ACTIVE.name || it.isPendingPersonalWallet,
                                            hideWalletDetail = hideWalletDetail,
                                            badgeCount = it.badgeCount,
                                            isLocked = it.group?.isLocked.orFalse(),
                                            primaryOwnerMember = it.primaryOwnerMember,
                                            role = it.role,
                                            status = it.keyStatus,
                                            signers = it.signers,
                                            useLargeFont = useLargeFont,
                                            walletStatus = briefWallet?.status,
                                            showShortcuts = state.homeDisplaySetting.showWalletShortcuts,
                                            onAccept = { onAccept(it) },
                                            onDeny = {
                                                denyInviteMemberGroupId = it.group?.id.orEmpty()
                                            },
                                            onGroupClick = {
                                                if (it.group?.id != null && it.role.toRole.isKeyHolderLimited && it.badgeCount == 0) return@PendingWalletView
                                                navigator.openGroupDashboardScreen(
                                                    groupId = it.group?.id,
                                                    walletId = it.wallet?.wallet?.id,
                                                    activityContext = activity
                                                )
                                            },
                                            onWalletClick = {
                                                if (it.role == AssistedWalletRole.KEYHOLDER_LIMITED.name || it.group?.isLocked == true) return@PendingWalletView
                                                val walletId =
                                                    it.wallet?.wallet?.id
                                                        ?: return@PendingWalletView
                                                if (briefWallet?.status == WalletStatus.REPLACED.name && briefWallet.replaceByWalletId.isNotEmpty()
                                                    && groupWalletUis.any { ui -> ui.wallet?.wallet?.id == briefWallet.replaceByWalletId }
                                                ) {
                                                    showWalletReplacedDialog(
                                                        walletId,
                                                        briefWallet.replaceByWalletId
                                                    )
                                                } else {
                                                    openWalletDetailsScreen(walletId)
                                                }
                                            },
                                            onSendClick = {
                                                it.wallet?.let { wallet ->
                                                    getWalletDetail(wallet.wallet.id)
                                                }
                                            },
                                            onReceiveClick = {
                                                val walletId = it.wallet?.wallet?.id
                                                if (!walletId.isNullOrEmpty()) {
                                                    navigator.openReceiveTransactionScreen(
                                                        activityContext = activity,
                                                        walletId = walletId
                                                    )
                                                }
                                            },
                                            onOpenFreeGroupWallet = {
                                                FreeGroupWalletActivity.start(
                                                    context = activity,
                                                    groupId = it.id
                                                )
                                            },
                                            isDeprecatedGroupWallet = deprecatedGroupWalletIds.contains(
                                                it.wallet?.wallet?.id
                                            )
                                        )
                                    }
                                }
                            }

                            if (state.totalArchivedWallet > 0) {
                                item {
                                    ArchivedWalletsRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        count = state.totalArchivedWallet,
                                        onClick = openArchivedWalletsScreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (denyInviteMemberGroupId.isNotEmpty()) {
            NcInfoDialog(
                title = stringResource(R.string.nc_text_confirmation),
                message = stringResource(R.string.nc_deny_wallet_invitation_dialog),
                positiveButtonText = stringResource(R.string.nc_text_yes),
                negativeButtonText = stringResource(R.string.nc_text_cancel),
                onDismiss = { denyInviteMemberGroupId = "" },
                onPositiveClick = {
                    denyInviteMember(denyInviteMemberGroupId)
                    denyInviteMemberGroupId = ""
                },
            )
        }
    }
}