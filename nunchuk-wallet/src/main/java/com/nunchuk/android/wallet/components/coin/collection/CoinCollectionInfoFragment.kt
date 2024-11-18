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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCheckBoxOption
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTextSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.collection.CoinCollectionInfoFragment.Companion.COINS_WITHOUT_TAGS
import com.nunchuk.android.wallet.components.coin.detail.component.TagHorizontalList
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.wallet.components.coin.tag.CoinTagListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinCollectionInfoFragment : Fragment() {

    private val viewModel: CoinCollectionInfoViewModel by viewModels()
    private val args: CoinCollectionInfoFragmentArgs by navArgs()
    private val coinListViewModel: CoinListViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(CoinTagListFragment.REQUEST_KEY) { _, bundle ->
            val tags = bundle.getIntArray(CoinTagListFragment.TAGS) ?: intArrayOf()
            viewModel.addTags(tags.toSet())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val coinListUiState by coinListViewModel.state.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            CreateCollectionView(
                initCollection = args.coinCollection,
                coinTags = coinListUiState.tags,
                state = uiState,
                onCreateCollection = { coinCollection, applyToExistingCoins ->
                    viewModel.createCoinCollection(coinCollection, applyToExistingCoins)
                },
                onViewTagDetail = { coinTag ->

                },
                onUpdateTag = {
                    findNavController().navigate(
                        CoinCollectionInfoFragmentDirections.actionCoinCollectionInfoFragmentToCoinTagListFragment(
                            walletId = args.walletId,
                            tagFlow = TagFlow.ADD_TO_COLLECTION,
                            coins = arrayOf(
                                UnspentOutput().apply {
                                    tags = uiState.selectedTags
                                }
                            )
                        )
                    )
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flowObserver(coinListViewModel.state) { coinListState ->
            viewModel.setCollections(coinListState.collections.values.toList())
        }
        flowObserver(viewModel.event) { event ->
            when (event) {
                is CoinCollectionBottomSheetEvent.Error -> showError(message = event.message)
                is CoinCollectionBottomSheetEvent.CreateOrUpdateCollectionSuccess -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(EXTRA_COIN_COLLECTION to event.collection)
                    )
                    findNavController().popBackStack()
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "CoinCollectionBottomSheetFragment"
        const val EXTRA_COIN_COLLECTION = "EXTRA_COIN_COLLECTION"
        const val COINS_WITHOUT_TAGS = -1
    }
}

@Composable
fun CreateCollectionView(
    initCollection: CoinCollection? = null,
    coinTags: Map<Int, CoinTag> = emptyMap(),
    state: CoinCollectionUiState = CoinCollectionUiState(),
    onCreateCollection: (CoinCollection, Boolean) -> Unit = {_, _ -> },
    onViewTagDetail: (CoinTag) -> Unit = {},
    onUpdateTag: () -> Unit = {},
) {
    var collectionName by rememberSaveable(initCollection) { mutableStateOf(initCollection?.name.orEmpty()) }
    var applyFilterToExistingCoins by rememberSaveable(initCollection) { mutableStateOf(false) }
    var autoLockCoin by rememberSaveable(initCollection) {
        mutableStateOf(
            initCollection?.isAutoLock ?: false
        )
    }
    var isCoinWithTag by rememberSaveable(initCollection) {
        mutableStateOf(initCollection?.tagIds.orEmpty().any { it != COINS_WITHOUT_TAGS })
    }
    var isCoinWithoutTag by rememberSaveable(initCollection) {
        mutableStateOf(initCollection?.tagIds.orEmpty().any { it == COINS_WITHOUT_TAGS })
    }
    var showNameExistError by rememberSaveable(state.isExist) { mutableStateOf(state.isExist) }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (initCollection != null) {
                        stringResource(id = R.string.nc_update_collection)
                    } else {
                        stringResource(id = R.string.nc_create_new_collection)
                    },
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        val tagIds = mutableSetOf<Int>()
                        if (isCoinWithTag) {
                            tagIds.addAll(state.selectedTags)
                        }
                        if (isCoinWithoutTag) {
                            tagIds.add(COINS_WITHOUT_TAGS)
                        }
                        val coinCollection = CoinCollection(
                            id = initCollection?.id ?: -1,
                            name = collectionName,
                            isAutoLock = autoLockCoin,
                            isAddNewCoin = isCoinWithTag && isCoinWithoutTag,
                            tagIds = tagIds
                        )
                        onCreateCollection(coinCollection, applyFilterToExistingCoins)
                    },
                    enabled = collectionName.isNotBlank()
                ) {
                    if (initCollection != null) {
                        Text(text = stringResource(id = R.string.nc_update_collection))
                    } else {
                        Text(text = stringResource(id = R.string.nc_create_collection))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                NcTextField(
                    title = "Collection name",
                    value = collectionName,
                    onValueChange = {
                        collectionName = it
                        showNameExistError = false
                    },
                    error = if (showNameExistError) stringResource(R.string.nc_collection_name_already_exists) else null
                )

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_filter),
                    style = NunchukTheme.typography.title,
                )

                NcCheckBoxOption(
                    modifier = Modifier.padding(top = 8.dp),
                    isSelected = isCoinWithoutTag,
                    onCheckedChange = {
                        isCoinWithoutTag = it
                    }
                ) {
                    Text(
                        text = stringResource(R.string.nc_coins_without_tags),
                        style = NunchukTheme.typography.title
                    )

                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.nc_coin_without_tags),
                        style = NunchukTheme.typography.body
                    )
                }

                NcCheckBoxOption(
                    modifier = Modifier.padding(top = 12.dp),
                    isSelected = isCoinWithTag,
                    onCheckedChange = {
                        isCoinWithTag = it
                    }
                ) {
                    Text(
                        text = stringResource(R.string.nc_coins_with_tags),
                        style = NunchukTheme.typography.title
                    )

                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.nc_coins_with_tags_desc),
                        style = NunchukTheme.typography.body
                    )
                }

                if (isCoinWithTag) {
                    TagHorizontalList(
                        modifier = Modifier.padding(top = 12.dp),
                        tags = state.selectedTags,
                        onUpdateTag = onUpdateTag,
                        coinTags = coinTags,
                        onViewTagDetail = onViewTagDetail
                    )
                }

                NcTextSwitch(
                    modifier = Modifier.padding(top = 12.dp),
                    title = stringResource(R.string.nc_apply_filter_to_existing_coins),
                    value = applyFilterToExistingCoins
                ) {
                    applyFilterToExistingCoins = it
                }

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_action),
                    style = NunchukTheme.typography.title,
                )

                NcTextSwitch(
                    modifier = Modifier.padding(top = 12.dp),
                    title = stringResource(R.string.nc_auto_lock_coins),
                    value = autoLockCoin
                ) {
                    autoLockCoin = it
                }
            }
        }
    }
}

@Preview
@Composable
private fun CreateCollectionViewPreview() {
    CreateCollectionView()
}
