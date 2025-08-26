/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.wallet.components.coin.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.coin.MODE_SELECT
import com.nunchuk.android.compose.coin.MODE_VIEW_DETAIL
import com.nunchuk.android.compose.coin.PreviewCoinCard
import com.nunchuk.android.compose.coin.TimelockTimeline
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.core.coin.CollectionFlow
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.fromSATtoBTC
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.base.BaseCoinListFragment
import com.nunchuk.android.wallet.components.coin.collection.CoinCollectionListFragment
import com.nunchuk.android.wallet.components.coin.component.CoinListBottomBar
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarNoneMode
import com.nunchuk.android.wallet.components.coin.component.CoinListTopBarSelectMode
import com.nunchuk.android.wallet.components.coin.tag.CoinTagSelectColorBottomSheetFragment
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CoinListFragment : BaseCoinListFragment() {
    private val args: CoinListFragmentArgs by navArgs()

    @Inject
    lateinit var navigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinListScreen(
                    viewModel = coinListViewModel,
                    args = args,
                    onViewCoinDetail = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinDetailFragment(
                                walletId = args.walletId, output = it
                            )
                        )
                    },
                    onSendBtc = {
                        val selectedCoins = coinListViewModel.getSelectedCoins()
                        if (selectedCoins.any { it.isLocked }) {
                            NCWarningDialog(requireActivity())
                                .showDialog(
                                    title = getString(R.string.nc_confirmation),
                                    message = getString(R.string.nc_locked_coin_can_not_used),
                                    onYesClick = {
                                        coinListViewModel.onUnlockCoin(
                                            args.walletId,
                                            selectedCoins,
                                            true
                                        )
                                    }
                                )
                        } else {
                            openCreateTransaction()
                        }
                    },
                    onShowSelectedCoinMoreOption = {
                        showSelectCoinOptions()
                    },
                    onShowMoreOptions = {
                        showMoreOptions()
                    },
                    enableSearchMode = {
                        findNavController().navigate(
                            CoinListFragmentDirections.actionCoinListFragmentToCoinSearchNavigation(
                                walletId = args.walletId,
                                isRelativeWallet = coinListViewModel.isRelativeTimelockWallet()
                            )
                        )
                    },
                    onViewTagDetail = {
                        findNavController().navigate(
                            CoinListFragmentDirections.actionCoinListFragmentToCoinTagDetailFragment(
                                walletId = args.walletId,
                                coinTag = it
                            )
                        )
                    }
                )
            }
        }
    }

    private fun openCreateTransaction() {
        navigator.openInputAmountScreen(
            activityContext = requireActivity(),
            walletId = args.walletId,
            availableAmount = coinListViewModel.getSelectedCoins()
                .sumOf { it.amount.value }.toDouble().fromSATtoBTC(),
            inputs = coinListViewModel.getSelectedCoins()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(CoinCollectionListFragment.REQUEST_KEY) { _, _ ->
            findNavController().popBackStack()
            clearFragmentResult(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY)
        }
        flowObserver(coinListViewModel.event) {
            if (it is CoinListEvent.CoinUnlocked && it.isCreateTransaction) {
                openCreateTransaction()
            }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.TYPE_VIEW_TAG -> findNavController().navigate(
                CoinNavigationDirections.actionGlobalCoinTagListFragment(
                    walletId = args.walletId, tagFlow = TagFlow.VIEW, coins = emptyArray()
                )
            )

            SheetOptionType.TYPE_VIEW_COLLECTION -> {
                findNavController().navigate(
                    CoinNavigationDirections.actionGlobalCoinCollectionListFragment(
                        walletId = args.walletId,
                        collectionFlow = CollectionFlow.VIEW,
                        coins = emptyArray()
                    )
                )
            }

            SheetOptionType.TYPE_VIEW_LOCKED_COIN -> {
                if (coinListViewModel.getLockedCoins().isEmpty()) {
                    NCInfoDialog(requireActivity()).showDialog(
                        message = getString(R.string.nc_locked_coins_empty_state),
                    ).show()
                } else {
                    findNavController().navigate(
                        CoinListFragmentDirections.actionCoinListFragmentSelf(
                            walletId = args.walletId,
                            listType = CoinListType.LOCKED,
                        )
                    )
                }
            }

            SheetOptionType.TYPE_REMOVE_COIN_FROM_TAG -> {
                coinListViewModel.removeCoinFromTag(args.walletId, args.tagId)
            }

            SheetOptionType.TYPE_REMOVE_COIN_FROM_COLLECTION -> {
                coinListViewModel.removeCoinFromCollection(args.walletId, args.collectionId)
            }

            SheetOptionType.TYPE_MOVE_COIN_TO_COLLECTION -> {
                findNavController().navigate(
                    CoinNavigationDirections.actionGlobalCoinCollectionListFragment(
                        walletId = walletId,
                        collectionFlow = CollectionFlow.MOVE,
                        coins = getSelectedCoins().toTypedArray()
                    )
                )
            }
        }
    }

    override fun showSelectCoinOptions() {
        if (args.tagId > 0) {
            showRemoveCoinFromTag()
        } else if (args.collectionId > 0) {
            showOptionCoinFromCollection()
        } else {
            super.showSelectCoinOptions()
        }
    }

    private fun showRemoveCoinFromTag() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_REMOVE_COIN_FROM_TAG,
                    label = getString(R.string.nc_remove_coin_from_this_tag)
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showOptionCoinFromCollection() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_REMOVE_COIN_FROM_COLLECTION,
                    label = getString(R.string.nc_remove_coin_from_this_collection)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_MOVE_COIN_TO_COLLECTION,
                    label = getString(R.string.nc_move_coin_to_another_collection)
                )
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showMoreOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_VIEW_TAG, label = getString(R.string.nc_view_tag)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_VIEW_COLLECTION,
                    label = getString(R.string.nc_view_collection)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_VIEW_LOCKED_COIN,
                    label = getString(R.string.nc_view_locked_coin)
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    override val walletId: String
        get() = args.walletId

    override fun getSelectedCoins(): List<UnspentOutput> {
        return coinListViewModel.getSelectedCoins()
    }

    override fun resetSelect() {
        coinListViewModel.resetSelect()
    }
}

@Composable
private fun CoinListScreen(
    viewModel: CoinListViewModel = viewModel(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onViewTagDetail: (tag: CoinTag) -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {},
    onSendBtc: () -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    enableSearchMode: () -> Unit = {},
    args: CoinListFragmentArgs,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val filterCoins by remember {
        derivedStateOf {
            when {
                args.tagId > 0 -> state.coins.filter { it.tags.contains(args.tagId) }
                args.collectionId > 0 -> state.coins.filter { it.collection.contains(args.collectionId) }
                args.txId.isNotEmpty() -> state.coins.filter { it.txid == args.txId }
                args.listType == CoinListType.LOCKED -> state.coins.filter { it.isLocked }
                else -> state.coins
            }
        }
    }

    CoinListContent(
        mode = state.mode,
        type = args.listType,
        coins = filterCoins,
        tags = state.tags,
        selectedCoin = state.selectedCoins,
        spendableAmount = state.spendableAmount,
        warningInfo = state.miniscriptWarningInfo,
        onSelectCoin = viewModel::onCoinSelect,
        onSelectOrUnselectAll = viewModel::onSelectOrUnselectAll,
        onSelectDone = viewModel::onSelectDone,
        enableSelectMode = viewModel::enableSelectMode,
        enableSearchMode = enableSearchMode,
        onViewCoinDetail = onViewCoinDetail,
        onViewTagDetail = onViewTagDetail,
        onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption,
        onSendBtc = onSendBtc,
        onShowMoreOptions = onShowMoreOptions,
    )
}

@Composable
private fun CoinListContent(
    mode: CoinListMode = CoinListMode.NONE,
    type: CoinListType = CoinListType.ALL,
    coins: List<UnspentOutput> = emptyList(),
    tags: Map<Int, CoinTag> = emptyMap(),
    selectedCoin: Set<UnspentOutput> = emptySet(),
    spendableAmount: Amount = Amount(0L),
    warningInfo: TimelockWarningInfo? = null,
    enableSelectMode: () -> Unit = {},
    enableSearchMode: () -> Unit = {},
    onSelectOrUnselectAll: (isSelect: Boolean, coins: List<UnspentOutput>) -> Unit = { _, _ -> },
    onSelectDone: () -> Unit = {},
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onViewTagDetail: (tag: CoinTag) -> Unit = {},
    onSendBtc: () -> Unit = {},
    onShowSelectedCoinMoreOption: () -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    onSelectCoin: (output: UnspentOutput, isSelected: Boolean) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState()
    val fabVisibility by remember {
        derivedStateOf {
            listState.isScrollInProgress.not()
        }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding(),
            topBar = {
                when (mode) {
                    CoinListMode.NONE -> {
                        CoinListTopBarNoneMode(
                            enableSelectMode = enableSelectMode,
                            onShowMoreOptions = onShowMoreOptions,
                            isShowMore = type == CoinListType.ALL,
                            title = if (type == CoinListType.ALL)
                                "${stringResource(R.string.nc_coins)} (${coins.size})"
                            else stringResource(R.string.nc_locked_coin)
                        )
                    }

                    CoinListMode.SELECT -> {
                        CoinListTopBarSelectMode(
                            isSelectAll = coins.size == selectedCoin.size,
                            onSelectOrUnselectAll = { isSelect ->
                                onSelectOrUnselectAll(
                                    isSelect,
                                    coins
                                )
                            },
                            onSelectDone = onSelectDone
                        )
                    }

                    CoinListMode.TRANSACTION_SELECT -> Unit
                }
            }, floatingActionButton = {
                if (mode == CoinListMode.NONE) {
                    AnimatedVisibility(
                        visible = fabVisibility,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        FloatingActionButton(
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.controlFillPrimary,
                            onClick = enableSearchMode
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search_white),
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.controlTextPrimary
                            )
                        }
                    }
                }
            }) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // Spendable amount section
                if (type == CoinListType.ALL && spendableAmount.value >= 0) {
                    SpendableAmountSection(spendableAmount = spendableAmount)
                }

                if (warningInfo != null && (warningInfo.hasAbsoluteTimelock || warningInfo.hasRelativeTimelock)) {
                    // Show warning info section only if there is a timelock warning
                    TimelockWarningSection(warningInfo = warningInfo)
                }

                // Show wallet timeline if wallet has absolute timelock
                if (warningInfo?.hasAbsoluteTimelock == true && !warningInfo.hasRelativeTimelock && coins.isNotEmpty()) {
                    TimelockTimeline(
                        output = coins.first(),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                    items(coins) { coin ->
                        PreviewCoinCard(
                            output = coin,
                            onSelectCoin = onSelectCoin,
                            isSelected = selectedCoin.contains(coin),
                            mode = if (mode == CoinListMode.SELECT) MODE_SELECT else MODE_VIEW_DETAIL,
                            onViewCoinDetail = onViewCoinDetail,
                            onViewTagDetail = onViewTagDetail,
                            tags = tags,
                            isShowTimeline = warningInfo?.hasRelativeTimelock == true
                        )
                    }
                }
                if (mode == CoinListMode.SELECT && selectedCoin.isNotEmpty()) {
                    CoinListBottomBar(
                        selectedCoin = selectedCoin,
                        onSendBtc = onSendBtc,
                        onShowSelectedCoinMoreOption = onShowSelectedCoinMoreOption,
                    )
                }
            }
        }
    }
}

@Composable
fun SpendableAmountSection(spendableAmount: Amount) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.lightGray)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nc_spendable_now_title),
                style = NunchukTheme.typography.titleSmall,
            )
            Text(
                text = spendableAmount.getBTCAmount(),
                style = NunchukTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun TimelockWarningSection(warningInfo: TimelockWarningInfo) {
    val warningMessage = when {
        warningInfo.hasRelativeTimelock && warningInfo.hasAbsoluteTimelock -> {
            "Move coins to reset a soon-expiring relative timelock. Absolute timelocks can't be reset, so after expiry, move funds to a new wallet to reuse signing policies."
        }

        warningInfo.hasRelativeTimelock -> {
            "Move coins with a soon-expiring timelock to another address in this wallet to reset the timelock."
        }

        warningInfo.hasAbsoluteTimelock -> {
            "Absolute timelocks can't be reset. After expiry, consider moving funds to a new wallet with the same policies if you want to reuse them."
        }

        else -> null
    }

    if (warningMessage != null) {
        NcHintMessage(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            type = HighlightMessageType.HINT
        ) {
            Text(
                text = warningMessage,
                style = NunchukTheme.typography.titleSmall,
            )
        }
    }
}

@Preview
@Composable
private fun CoinListScreenPreview() {
    val coin = UnspentOutput(
        amount = Amount(1000000L),
        isLocked = true,
        scheduleTime = System.currentTimeMillis(),
        time = System.currentTimeMillis(),
        tags = setOf(),
        memo = "Send to Bob on Silk Road",
        status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
    )
    CoinListContent(
        coins = listOf(
            coin.copy(vout = 1),
            coin.copy(vout = 2),
            coin.copy(vout = 3),
            coin.copy(vout = 4),
            coin.copy(vout = 5)
        ),
        spendableAmount = Amount(1000000L),
        warningInfo = TimelockWarningInfo(
            hasAbsoluteTimelock = true
        ),
    )
}