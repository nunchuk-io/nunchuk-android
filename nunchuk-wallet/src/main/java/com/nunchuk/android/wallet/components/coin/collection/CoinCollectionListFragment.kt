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

package com.nunchuk.android.wallet.components.coin.collection

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.CollectionItem
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.coin.CollectionFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinCollectionAddition
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.wallet.components.coin.tag.CoinTagSelectColorBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinCollectionListFragment : Fragment() {

    private val viewModel: CoinCollectionListViewModel by viewModels()
    private val coinListViewModel: CoinListViewModel by activityViewModels()
    private val args: CoinCollectionListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinCollectionListScreen(
                    viewModel,
                    collectionFlow = args.collectionFlow,
                    onCreateNewCollectionClick = {
                        findNavController().navigate(
                            CoinCollectionListFragmentDirections.actionCoinCollectionListFragmentToCollectionIntroFragment(
                                args.walletId
                            )
                        )
                    },
                    onCollectionClick = {
                        findNavController().navigate(
                            CoinCollectionListFragmentDirections.actionCoinCollectionListFragmentToCoinCollectionDetailFragment(
                                walletId = args.walletId, coinCollection = it.collection
                            )
                        )
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is CoinCollectionListEvent.Error -> showError(message = event.message)
                is CoinCollectionListEvent.Loading -> showOrHideLoading(loading = event.show)
                is CoinCollectionListEvent.AddCoinToCollectionSuccess -> {
                    handleTagInfoChange()
                    showSuccess(
                        message = if (event.numsCoin > 1) getString(R.string.nc_coins_updated) else getString(
                            R.string.nc_coin_updated
                        )
                    )
                    if (args.collectionFlow == CollectionFlow.MOVE) {
                        setFragmentResult(REQUEST_KEY, bundleOf())
                    }
                    findNavController().popBackStack()
                }
            }
        }

        flowObserver(coinListViewModel.state) { coinListState ->
            val numberOfCoinByCollectionId = mutableMapOf<Int, Int>()
            coinListState.coins.forEach { output ->
                output.collection.forEach { collectionId ->
                    numberOfCoinByCollectionId[collectionId] =
                        numberOfCoinByCollectionId.getOrPut(collectionId) { 0 } + 1
                }
            }
            viewModel.updateCoins(
                coinListState.collections.values.toList(), numberOfCoinByCollectionId
            )
        }

        setFragmentResultListener(CoinCollectionInfoFragment.REQUEST_KEY) { _, _ ->
            handleTagInfoChange()
            clearFragmentResult(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY)
        }

    }

    private fun handleTagInfoChange() {
        coinListViewModel.refresh()
        requireActivity().setResult(Activity.RESULT_OK)
    }

    companion object {
        const val REQUEST_KEY = "CoinCollectionListFragment"
    }
}

@Composable
fun CoinCollectionListScreen(
    viewModel: CoinCollectionListViewModel = viewModel(),
    @CollectionFlow.CollectionFlowInfo collectionFlow: Int = CollectionFlow.NONE,
    onCollectionClick: (CoinCollectionAddition) -> Unit = {},
    onCreateNewCollectionClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoinCollectionListScreenContent(
        collections = state.collections.sortedBy { it.collection.name },
        collectionFlow = collectionFlow,
        enableSaveButton = viewModel.enableButtonSave(),
        onCreateNewCollectionClick = onCreateNewCollectionClick,
        selectedCoinCollections = state.selectedCoinCollections,
        onCollectionClick = onCollectionClick,
        onCheckedChange = { id, checked ->
            viewModel.onCheckedChange(id, checked)
        },
        onSaveClick = {
            viewModel.addCoinCollection()
        },
    )
}

@Composable
fun CoinCollectionListScreenContent(
    collections: List<CoinCollectionAddition> = emptyList(),
    collectionFlow: Int = CollectionFlow.NONE,
    selectedCoinCollections: Set<Int> = hashSetOf(),
    enableSaveButton: Boolean = false,
    onSaveClick: () -> Unit = {},
    onCreateNewCollectionClick: () -> Unit = {},
    onCollectionClick: (CoinCollectionAddition) -> Unit = {},
    onCheckedChange: ((Int, Boolean) -> Unit) = { _, _ -> }
) {
    val context = LocalContext.current
    val title = remember(collectionFlow) {
        when (collectionFlow) {
            CollectionFlow.ADD -> context.getString(R.string.nc_add_to_collection)
            CollectionFlow.VIEW -> context.getString(R.string.nc_collections)
            CollectionFlow.MOVE -> context.getString(R.string.nc_move_to_another_collection)
            else -> throw IllegalArgumentException("invalid flow")
        }
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = title,
                    textStyle = NunchukTheme.typography.titleLarge,
                    isBack = false,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    },
                )
            }, bottomBar = {
                if (collectionFlow == CollectionFlow.ADD || collectionFlow == CollectionFlow.MOVE) {
                    NcPrimaryDarkButton(
                        enabled = enableSaveButton,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onSaveClick,
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_save))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                        .clickable {
                            onCreateNewCollectionClick()
                        },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_add_circle),
                        contentDescription = ""
                    )
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = stringResource(id = R.string.nc_create_a_new_collection),
                        style = NunchukTheme.typography.title
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(collections) { collection ->
                        CollectionItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            id = collection.collection.id,
                            name = collection.collection.name,
                            numCoins = collection.numCoins,
                            checked = selectedCoinCollections.contains(collection.collection.id),
                            onCollectionClick = { onCollectionClick(collection) },
                            collectionFlow = collectionFlow,
                            onCheckedChange = {
                                onCheckedChange(collection.collection.id, it)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CoinCollectionListScreenContentPreview() {
    val collections = arrayListOf<CoinCollectionAddition>()
    collections.apply {
        add(CoinCollectionAddition(CoinCollection(name = "Collection A")))
        add(CoinCollectionAddition(CoinCollection(name = "Collection B")))
        add(CoinCollectionAddition(CoinCollection(name = "Collection C")))
        add(CoinCollectionAddition(CoinCollection(name = "Collection D")))
    }
    CoinCollectionListScreenContent(collections = collections, collectionFlow = CollectionFlow.ADD)
}