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

package com.nunchuk.android.wallet.components.coin.detail

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.collection.CollectionFlow
import com.nunchuk.android.wallet.components.coin.component.CoinBadge
import com.nunchuk.android.wallet.components.coin.detail.component.CoinTransactionCard
import com.nunchuk.android.wallet.components.coin.detail.component.CollectionHorizontalList
import com.nunchuk.android.wallet.components.coin.detail.component.LockCoinRow
import com.nunchuk.android.wallet.components.coin.detail.component.TagHorizontalList
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CoinDetailFragment : Fragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator
    private val viewModel: CoinDetailViewModel by viewModels()
    private val coinViewModel: CoinListViewModel by activityViewModels()
    private val args by navArgs<CoinDetailFragmentArgs>()

    private val transactionDetailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.getTransactionDetail()
                coinViewModel.refresh()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinDetailScreen(
                    viewModel = viewModel,
                    coinViewModel = coinViewModel,
                    args = args,
                    onShowMore = {
                        BottomSheetOption.newInstance(
                            listOf(
                                SheetOption(
                                    type = SheetOptionType.TYPE_SHOW_OUTPOINT,
                                    label = context.getString(R.string.nc_show_out_point)
                                )
                            )
                        ).show(childFragmentManager, "BottomSheetOption")
                    },
                    onViewTransactionDetail = {
                        navigator.openTransactionDetailsScreen(
                            launcher = transactionDetailLauncher,
                            activityContext = requireActivity(),
                            walletId = args.walletId,
                            txId = args.output.txid,
                        )
                    },
                    onUpdateTag = {
                        findNavController().navigate(
                            CoinDetailFragmentDirections.actionCoinDetailFragmentToCoinTagListFragment(
                                walletId = args.walletId,
                                tagFlow = TagFlow.ADD,
                                coins = arrayOf(it)
                            )
                        )
                    },
                    onUpdateCollection = {
                        findNavController().navigate(
                            CoinDetailFragmentDirections.actionCoinDetailFragmentToCoinCollectionListFragment(
                                walletId = args.walletId,
                                collectionFlow = CollectionFlow.ADD,
                                coins = arrayOf(it)
                            )
                        )
                    },
                    onViewTagDetail = {
                        findNavController().navigate(
                            CoinDetailFragmentDirections.actionCoinDetailFragmentToCoinTagDetailFragment(
                                walletId = args.walletId,
                                coinTag = it
                            )
                        )
                    },
                    onViewCollectionDetail = {
                        findNavController().navigate(
                            CoinDetailFragmentDirections.actionCoinDetailFragmentToCoinCollectionDetailFragment(
                                walletId = args.walletId,
                                coinCollection = it
                            )
                        )
                    },
                    onViewCoinAncestry = { output ->
                        findNavController().navigate(
                            CoinDetailFragmentDirections.actionCoinDetailFragmentToCoinAncestryFragment(
                                walletId = args.walletId,
                                output = output
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_SHOW_OUTPOINT ->
                OutpointBottomSheet.newInstance("${args.output.txid}:${args.output.vout}")
                    .show(
                        childFragmentManager, "OutpointBottomSheet"
                    )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { event ->
                when (event) {
                    is CoinDetailEvent.LockOrUnlockSuccess -> {
                        if (event.isLocked) {
                            showSuccess(getString(R.string.nc_one_coin_locked))
                        }
                        coinViewModel.refresh()
                    }

                    is CoinDetailEvent.ShowError -> showError(event.message)
                }
            }
        }
    }
}

@Composable
private fun CoinDetailScreen(
    viewModel: CoinDetailViewModel = viewModel(),
    coinViewModel: CoinListViewModel,
    args: CoinDetailFragmentArgs,
    onShowMore: () -> Unit = {},
    onViewTransactionDetail: () -> Unit = {},
    onUpdateTag: (output: UnspentOutput) -> Unit,
    onUpdateCollection: (output: UnspentOutput) -> Unit,
    onViewTagDetail: (tag: CoinTag) -> Unit = {},
    onViewCollectionDetail: (collection: CoinCollection) -> Unit = {},
    onViewCoinAncestry: (output: UnspentOutput) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coinListState by coinViewModel.state.collectAsStateWithLifecycle()

    val output =
        coinListState.coins.find { it.txid == args.output.txid && it.vout == args.output.vout } ?: args.output

    CoinDetailContent(
        output = output,
        transaction = state.transaction,
        onShowMore = onShowMore,
        onViewTransactionDetail = onViewTransactionDetail,
        onUpdateTag = onUpdateTag,
        onUpdateCollection = onUpdateCollection,
        coinCollections = coinListState.collections,
        coinTags = coinListState.tags,
        onViewTagDetail = onViewTagDetail,
        onViewCollectionDetail = onViewCollectionDetail,
        onLockOrUnlock = viewModel::lockCoin,
        onViewCoinAncestry = onViewCoinAncestry,
        isSpentCoin = args.isSpent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoinDetailContent(
    output: UnspentOutput = UnspentOutput(),
    isSpentCoin: Boolean = false,
    coinTags: Map<Int, CoinTag> = emptyMap(),
    coinCollections: Map<Int, CoinCollection> = emptyMap(),
    transaction: Transaction = Transaction(),
    onShowMore: () -> Unit = {},
    onViewTransactionDetail: () -> Unit = {},
    onUpdateTag: (output: UnspentOutput) -> Unit = {},
    onUpdateCollection: (output: UnspentOutput) -> Unit = {},
    onViewTagDetail: (tag: CoinTag) -> Unit = {},
    onViewCollectionDetail: (collection: CoinCollection) -> Unit = {},
    onLockOrUnlock: (isLocked: Boolean) -> Unit = {},
    onViewCoinAncestry: (output: UnspentOutput) -> Unit = {},
) {
    val backgroundColor = if (isSpentCoin) MaterialTheme.colorScheme.whisper else MaterialTheme.colorScheme.denimTint
    NunchukTheme {
        Scaffold(topBar = {
            Box(
                modifier = Modifier
                    .background(color = backgroundColor)
                    .statusBarsPadding()
            ) {
                NcTopAppBar(
                    title = stringResource(R.string.nc_coin_detail),
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        IconButton(onClick = onShowMore) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More"
                            )
                        }
                    },
                    backgroundColor = backgroundColor
                    )
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .background(color = backgroundColor)
                ) {
                    CoinBadgeRow(output)
                    Text(
                        text = output.amount.getBTCAmount(),
                        style = NunchukTheme.typography.heading,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = output.amount.getCurrencyAmount(),
                        style = NunchukTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = output.time.getBtcFormatDate(),
                            style = NunchukTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                        )
                        CoinStatusBadge(output)
                    }
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(R.string.nc_parent_transaction),
                        style = NunchukTheme.typography.title,
                    )

                    CoinTransactionCard(
                        transaction = transaction,
                        onViewTransactionDetail = onViewTransactionDetail
                    )

                    if (isSpentCoin.not()) {
                        NcOutlineButton(
                            modifier = Modifier
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 24.dp
                                )
                                .fillMaxWidth(), onClick = { onViewCoinAncestry(output) }) {
                            Text(text = "View coin ancestry", style = NunchukTheme.typography.title)
                        }
                    }
                }

                if (isSpentCoin.not()) {
                    LockCoinRow(output = output, onLockCoin = onLockOrUnlock)

                    TagHorizontalList(
                        modifier = Modifier.padding(top = 8.dp),
                        output = output,
                        onUpdateTag = onUpdateTag,
                        coinTags = coinTags,
                        onViewTagDetail = onViewTagDetail
                    )

                    CollectionHorizontalList(
                        modifier = Modifier.padding(top = 8.dp),
                        output = output,
                        onUpdateCollection = onUpdateCollection,
                        coinCollections = coinCollections,
                        onViewCollectionDetail = onViewCollectionDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun CoinBadgeRow(output: UnspentOutput) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp, horizontal = 12.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        if (output.isChange) {
            CoinBadge(modifier = Modifier.padding(end = 4.dp)) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    text = stringResource(R.string.nc_change),
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 12.sp)
                )
            }
        }

        if (output.isLocked) {
            CoinBadge(
                modifier = Modifier.padding(end = 4.dp),
                border = 0.dp,
                backgroundColor = NcColor.whisper
            ) {
                Icon(
                    modifier = Modifier.padding(start = 10.dp),
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Locked"
                )
                Text(
                    modifier = Modifier.padding(
                        start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp
                    ),
                    text = stringResource(R.string.nc_locked),
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 12.sp)
                )
            }
        }

        if (output.scheduleTime > 0L) {
            CoinBadge(
                border = 0.dp,
                backgroundColor = NcColor.whisper
            ) {
                Icon(
                    modifier = Modifier.padding(start = 10.dp),
                    painter = painterResource(id = R.drawable.ic_schedule),
                    contentDescription = "Locked"
                )
                Text(
                    modifier = Modifier.padding(
                        start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp
                    ),
                    text = stringResource(R.string.nc_scheduled_transaction),
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 12.sp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CoinDetailScreenPreview() {
    NunchukTheme {
        CoinDetailContent(

        )
    }
}