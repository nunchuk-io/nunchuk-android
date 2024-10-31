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

package com.nunchuk.android.wallet.components.coin.tagdetail

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.PreviewCoinCard
import com.nunchuk.android.compose.fillDenim
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.hexToColor
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.list.CoinListType
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.wallet.components.coin.tag.CoinTagColorUtil
import com.nunchuk.android.wallet.components.coin.tag.CoinTagSelectColorBottomSheetFragment
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe

@AndroidEntryPoint
class CoinTagDetailFragment : Fragment(), BottomSheetOptionListener {
    private val viewModel: CoinTagDetailViewModel by viewModels()
    private val coinListViewModel: CoinListViewModel by activityViewModels()
    private val args: CoinTagDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CoinTagDetailScreen(
                    viewModel = viewModel,
                    onViewCoinDetail = {
                        findNavController().navigate(
                            CoinNavigationDirections.actionGlobalCoinDetailFragment(
                                walletId = args.walletId,
                                output = it
                            )
                        )
                    }, onEditTagNameClick = {
                        findNavController().navigate(
                            CoinTagDetailFragmentDirections.actionCoinTagDetailFragmentToEditTagNameBottomSheetFragment(
                                walletId = args.walletId,
                                coinTag = it
                            )
                        )
                    }, onEditTagColorClick = {
                        findNavController().navigate(
                            CoinTagDetailFragmentDirections.actionCoinTagDetailFragmentToCoinTagSelectColorBottomSheetFragment(
                                selectedColor = it
                            )
                        )
                    }, onShowMoreOptions = {
                        showDeleteTagOption()
                    },
                    onRemoveCoin = { coin ->
                        NCWarningDialog(requireActivity()).showDialog(
                            title = getString(R.string.nc_confirmation),
                            message = getString(
                                R.string.nc_remove_coin_confirmation,
                                args.coinTag.name
                            ), onYesClick = {
                                viewModel.removeCoin(listOf(coin))
                            })
                    },
                    enableSelectMode = {
                        findNavController().navigate(
                            CoinTagDetailFragmentDirections.actionCoinTagDetailFragmentToCoinListFragment(
                                walletId = args.walletId,
                                listType = CoinListType.ALL,
                                tagId = args.coinTag.id
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
                is CoinTagDetailEvent.Error -> showError(message = event.message)
                is CoinTagDetailEvent.Loading -> showOrHideLoading(loading = event.show)
                CoinTagDetailEvent.DeleteTagSuccess -> {
                    NcToastManager.scheduleShowMessage(message = getString(R.string.nc_tag_deleted))
                    handleTagInfoChange()
                    findNavController().popBackStack()
                }

                CoinTagDetailEvent.UpdateTagColorSuccess -> {
                    handleTagInfoChange()
                    showTagUpdated()
                }

                CoinTagDetailEvent.RemoveCoinSuccess -> {
                    handleTagInfoChange()
                    showTagUpdated()
                }
            }
        }

        flowObserver(coinListViewModel.state) { state ->
            viewModel.getListCoinByTag(state.coins, state.tags)
        }

        setFragmentResultListener(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            bundle.getString(CoinTagSelectColorBottomSheetFragment.EXTRA_SELECT_COLOR)
                ?.let {
                    viewModel.updateColor(it)
                } ?: run {
                clearFragmentResult(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY)
            }
        }

        setFragmentResultListener(EditTagNameBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            bundle.getString(EditTagNameBottomSheetFragment.EXTRA_COIN_TAG_NAME)
                ?.let {
                    viewModel.updateTagName(it)
                    showTagUpdated()
                    handleTagInfoChange()
                } ?: run {
                clearFragmentResult(EditTagNameBottomSheetFragment.REQUEST_KEY)
            }
        }
    }

    private fun handleTagInfoChange() {
        coinListViewModel.refresh()
        requireActivity().setResult(Activity.RESULT_OK)
    }

    private fun showTagUpdated() {
        showSuccess(message = getString(R.string.nc_tag_updated))
    }

    private fun showDeleteTagOption() {
        (childFragmentManager.findFragmentByTag("BottomSheetOption") as? DialogFragment)?.dismiss()
        val dialog = BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_DELETE_TAG,
                    R.drawable.ic_delete_red,
                    R.string.nc_delete_tag,
                    isDeleted = true
                ),
            )
        )
        dialog.show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        if (viewModel.getNumCoins() > 0) {
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(
                    R.string.nc_delete_tag_confirmation,
                    viewModel.getNumCoins().toString()
                ), onYesClick = {
                    viewModel.deleteCoinTag()
                })
        } else {
            viewModel.deleteCoinTag()
        }
    }

    companion object {
        const val REQUEST_KEY = "CoinTagDetailFragment"
    }
}

@Composable
private fun CoinTagDetailScreen(
    viewModel: CoinTagDetailViewModel = viewModel(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onEditTagNameClick: (CoinTag) -> Unit = {},
    onEditTagColorClick: (String) -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    onRemoveCoin: (UnspentOutput) -> Unit = {},
    enableSelectMode: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coinTag = state.coinTag ?: return

    CoinTagDetailContent(
        coins = state.coins,
        tags = state.tags,
        coinTag = coinTag,
        onViewCoinDetail = onViewCoinDetail,
        onEditTagNameClick = {
            onEditTagNameClick(coinTag)
        },
        onEditTagColorClick = {
            onEditTagColorClick(coinTag.color)
        },
        onShowMoreOptions = onShowMoreOptions,
        onRemoveCoin = onRemoveCoin,
        enableSelectMode = enableSelectMode
    )
}

@Composable
private fun CoinTagDetailContent(
    coins: List<UnspentOutput> = emptyList(),
    tags: Map<Int, CoinTag> = emptyMap(),
    coinTag: CoinTag = CoinTag(),
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    enableSelectMode: () -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    onEditTagNameClick: () -> Unit = {},
    onEditTagColorClick: () -> Unit = {},
    onRemoveCoin: (UnspentOutput) -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.fillDenim)
                        .statusBarsPadding()
                ) {
                    NcTopAppBar(
                        title = "",
                        backgroundColor = MaterialTheme.colorScheme.fillDenim,
                        textStyle = NunchukTheme.typography.titleLarge,
                        isBack = true,
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
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                Column {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        item {
                            Column(
                                Modifier
                                    .background(color = MaterialTheme.colorScheme.fillDenim)
                                    .padding(vertical = 24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp, 96.dp)
                                        .clip(CircleShape)
                                        .background(color = coinTag.color.hexToColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_select_color),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(width = 36.dp, height = 36.dp)
                                            .clickable {
                                                onEditTagColorClick()
                                            }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        modifier = Modifier.weight(1f, fill = false),
                                        text = coinTag.name,
                                        style = NunchukTheme.typography.heading
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
                                    text = stringResource(id = R.string.nc_tags_empty_state),
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
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            output = output,
            onViewCoinDetail = onViewCoinDetail,
            tags = tags,
        )
    }
}

@Preview
@Composable
private fun CoinTagDetailScreenPreview() {
    val coin = UnspentOutput(
        amount = Amount(1000000L),
        isLocked = true,
        scheduleTime = System.currentTimeMillis(),
        time = System.currentTimeMillis(),
        tags = setOf(),
        memo = "Send to Bob on Silk Road",
        status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
    )
    CoinTagDetailContent(
        coins = listOf(
            coin.copy(vout = 1),
            coin.copy(vout = 2),
            coin.copy(vout = 3),
            coin.copy(vout = 4),
            coin.copy(vout = 5)
        ),
        coinTag = CoinTag(
            name = "#aaa",
            color = CoinTagColorUtil.hexColors.first()
        )

    )
}