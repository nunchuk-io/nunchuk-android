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

package com.nunchuk.android.wallet.components.coin.collectiondetail

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.PreviewCoinCard
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.collection.CoinCollectionBottomSheetFragment
import com.nunchuk.android.wallet.components.coin.collection.CollectionFlow
import com.nunchuk.android.wallet.components.coin.list.CoinListType
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.wallet.components.coin.tag.CoinTagSelectColorBottomSheetFragment
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe

@AndroidEntryPoint
class CoinCollectionDetailFragment : Fragment(), BottomSheetOptionListener {
    private val viewModel: CoinCollectionDetailViewModel by viewModels()
    private val coinListViewModel: CoinListViewModel by activityViewModels()
    private val args: CoinCollectionDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinCollectionDetailScreen(
                    viewModel = viewModel,
                    onViewCoinDetail = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinDetailFragment(
                                walletId = args.walletId,
                                output = it,
                            )
                        )
                    }, onShowMoreOptions = {
                        showDeleteCollectionOption()
                    },
                    onRemoveCoin = { coin ->
                        NCWarningDialog(requireActivity()).showDialog(
                            title = getString(R.string.nc_confirmation),
                            message = getString(
                                R.string.nc_remove_coin_confirmation,
                                args.coinCollection.name
                            ), onYesClick = {
                                viewModel.removeCoin(listOf(coin))
                            })
                    },
                    onEditCollectionNameClick = {
                        showCoinCollectionSetting()
                    },
                    enableSelectMode = {
                        findNavController().navigate(
                            CoinCollectionDetailFragmentDirections.actionCoinCollectionDetailFragmentToCoinListFragment(
                                walletId = args.walletId,
                                listType = CoinListType.ALL,
                                collectionId = args.coinCollection.id
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is CoinCollectionDetailEvent.Error -> showError(message = event.message)
                is CoinCollectionDetailEvent.Loading -> showOrHideLoading(loading = event.show)
                CoinCollectionDetailEvent.DeleteCollectionSuccess -> {
                    NcToastManager.scheduleShowMessage(message = getString(R.string.nc_collection_deleted))
                    handleTagInfoChange()
                    findNavController().popBackStack()
                }

                CoinCollectionDetailEvent.RemoveCoinSuccess -> handleTagInfoChange()
            }
        }

        flowObserver(coinListViewModel.state) { state ->
            viewModel.getListCoinByTag(state.coins, state.tags)
        }

        setFragmentResultListener(CoinCollectionBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            bundle.parcelable<CoinCollection>(CoinCollectionBottomSheetFragment.EXTRA_COIN_COLLECTION)
                ?.let {
                    viewModel.updateCoinCollection(it)
                    showCollectionUpdated()
                    handleTagInfoChange()
                } ?: run {
                clearFragmentResult(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY)
            }
        }
    }

    private fun handleTagInfoChange() {
        coinListViewModel.refresh()
        requireActivity().setResult(Activity.RESULT_OK)
    }

    private fun showCollectionUpdated() {
        showSuccess(message = getString(R.string.nc_collection_updated))
    }

    private fun showDeleteCollectionOption() {
        (childFragmentManager.findFragmentByTag("BottomSheetOption") as? DialogFragment)?.dismiss()
        val dialog = BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_VIEW_COLLECTION_SETTING,
                    R.drawable.ic_settings_dark,
                    R.string.nc_view_collection_settings,
                ),
                SheetOption(
                    SheetOptionType.TYPE_DELETE_COLLECTION,
                    R.drawable.ic_delete_red,
                    R.string.nc_delete_collection,
                    isDeleted = true
                )
            )
        )
        dialog.show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_VIEW_COLLECTION_SETTING -> {
                showCoinCollectionSetting()
            }

            SheetOptionType.TYPE_DELETE_COLLECTION -> {
                if (viewModel.getNumCoins() > 0) {
                    NCWarningDialog(requireActivity()).showDialog(
                        title = getString(R.string.nc_confirmation),
                        message = getString(
                            R.string.nc_delete_collection_confirmation,
                            viewModel.getNumCoins().toString()
                        ), onYesClick = {
                            viewModel.deleteCoinCollection()
                        })
                } else {
                    viewModel.deleteCoinCollection()
                }
            }
        }

    }

    private fun showCoinCollectionSetting() {
        findNavController().navigate(
            CoinNavigationDirections.actionGlobalCoinCollectionBottomSheetFragment(
                walletId = args.walletId,
                coinCollection = viewModel.getCoinCollection(),
                flow = CollectionFlow.VIEW,
            )
        )
    }

    companion object {
        const val REQUEST_KEY = "CoinCollectionDetailFragment"
    }
}

@Composable
private fun CoinCollectionDetailScreen(
    viewModel: CoinCollectionDetailViewModel = viewModel(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onEditCollectionNameClick: (CoinCollection) -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    onRemoveCoin: (UnspentOutput) -> Unit = {},
    enableSelectMode: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coinCollection = state.coinCollection ?: return

    CoinCollectionDetailContent(
        coins = state.coins,
        tags = state.tags,
        coinCollection = coinCollection,
        onViewCoinDetail = onViewCoinDetail,
        onEditTagNameClick = {
            onEditCollectionNameClick(coinCollection)
        },
        onShowMoreOptions = onShowMoreOptions,
        onRemoveCoin = onRemoveCoin,
        enableSelectMode = enableSelectMode
    )
}

@Composable
private fun CoinCollectionDetailContent(
    coins: List<UnspentOutput> = emptyList(),
    tags: Map<Int, CoinTag> = emptyMap(),
    coinCollection: CoinCollection = CoinCollection(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    enableSelectMode: () -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    onEditTagNameClick: () -> Unit = {},
    onEditTagColorClick: () -> Unit = {},
    onRemoveCoin: (UnspentOutput) -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .background(colorResource(id = R.color.nc_beeswax_tint))
                        .statusBarsPadding()
                ) {
                    NcTopAppBar(
                        title = "",
                        backgroundColor = colorResource(id = R.color.nc_beeswax_tint),
                        textStyle = NunchukTheme.typography.titleLarge,
                        isBack = true,
                        elevation = 0.dp,
                        actions = {
                            if (coins.isEmpty().not()) {
                                Text(
                                    modifier = Modifier.clickable { enableSelectMode() },
                                    text = stringResource(R.string.nc_select),
                                    style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                                )
                            }
                            IconButton(onClick = onShowMoreOptions) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        },
                    )
                }

                Column {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(innerPadding)
                    ) {
                        item {
                            Column(
                                Modifier
                                    .background(color = colorResource(id = R.color.nc_beeswax_tint))
                                    .padding(vertical = 24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ConstraintLayout {
                                    val (avatar, editAvatar) = createRefs()
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp, 80.dp)
                                            .clip(CircleShape)
                                            .background(color = colorResource(id = R.color.nc_beeswax_light))
                                            .constrainAs(avatar) {},
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = coinCollection.name.shorten(),
                                            style = NunchukTheme.typography.heading
                                        )
                                    }
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_capture_avatar_circle),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(width = 36.dp, height = 36.dp)
                                            .clickable {
                                                onEditTagColorClick()
                                            }
                                            .constrainAs(editAvatar) {
                                                end.linkTo(avatar.end)
                                                bottom.linkTo(avatar.bottom)
                                            }
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(top = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = coinCollection.name,
                                        style = NunchukTheme.typography.heading,
                                        textAlign = TextAlign.Center
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_edit),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .size(18.dp)
                                            .clickable {
                                                onEditTagNameClick()
                                            }
                                    )
                                }

                                val textValue = stringResource(id = R.string.nc_num_coins_data, coins.size)
                                val totalAmount = coins.sumOf { it.amount.value }
                                val amountTotalText = if (LocalView.current.isInEditMode)
                                    "$totalAmount sats"
                                else
                                    Amount(totalAmount).getBTCAmount()

                                Text(
                                    text = "$textValue ($amountTotalText)", style = NunchukTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        if (coins.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(id = R.string.nc_collections_empty_state),
                                    modifier = Modifier.padding(
                                        top = 32.dp,
                                        start = 16.dp,
                                        end = 16.dp
                                    ),
                                    textAlign = TextAlign.Center,
                                    style = NunchukTheme.typography.body
                                )
                            }
                        } else {
                            items(coins) { coin ->
                                SwipeDismissPreviewCoinCard(
                                    output = coin,
                                    tags = tags,
                                    onViewCoinDetail = onViewCoinDetail,
                                    onDeleteCoin = {
                                        onRemoveCoin(coin)
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeDismissPreviewCoinCard(
    output: UnspentOutput,
    tags: Map<Int, CoinTag>,
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onDeleteCoin: () -> Unit = {}
) {
    RevealSwipe(
        maxAmountOfOverflow = 96.dp,
        backgroundCardEndColor = colorResource(id = R.color.nc_orange_color),
        directions = setOf(RevealDirection.EndToStart),
        hiddenContentEnd = {
            Icon(
                modifier = Modifier.padding(horizontal = 25.dp),
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = null,
                tint = Color.White
            )
        },
        onBackgroundEndClick = onDeleteCoin
    ) {
        PreviewCoinCard(
            modifier = Modifier.background(Color.White),
            output = output,
            onViewCoinDetail = onViewCoinDetail,
            tags = tags,
        )
    }
}

@Preview
@Composable
private fun CoinCollectionDetailScreenPreview() {
    val coin = UnspentOutput(
        amount = Amount(1000000L),
        isLocked = true,
        scheduleTime = System.currentTimeMillis(),
        time = System.currentTimeMillis(),
        tags = setOf(),
        memo = "Send to Bob on Silk Road",
        status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
    )
    CoinCollectionDetailContent(
        coins = listOf(
            coin.copy(vout = 1),
            coin.copy(vout = 2),
            coin.copy(vout = 3),
            coin.copy(vout = 4),
            coin.copy(vout = 5)
        ),
        coinCollection = CoinCollection(
            name = "Unfiltered coins"
        )

    )
}