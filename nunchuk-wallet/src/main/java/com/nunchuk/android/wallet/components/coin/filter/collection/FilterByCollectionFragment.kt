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

package com.nunchuk.android.wallet.components.coin.filter.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.CollectionItem
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.CoinCollectionAddition
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterByCollectionFragment : Fragment() {
    private val viewModel: FilterByCollectionViewModel by viewModels()
    private val coinListViewModel: CoinListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FilterByCollectionScreen(viewModel) {
                    setFragmentResult(
                        REQUEST_KEY,
                        FilterByCollectionFragmentArgs(viewModel.getSelectCollections().toIntArray()).toBundle()
                    )
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flowObserver(coinListViewModel.state) {
            viewModel.extractTagAndNumberOfCoin(it.coins, it.collections.values.toList())
        }
    }

    companion object {
        const val REQUEST_KEY = "FilterByCollectionFragment"
    }
}

@Composable
private fun FilterByCollectionScreen(
    viewModel: FilterByCollectionViewModel = viewModel(),
    onSelectDone: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    FilterByCollectionContent(
        collections = state.allCollections,
        previousSelectedCollectionIds = state.previousCollectionIds,
        selectedCollectionIds = state.selectedIds,
        onCheckedChange = viewModel::onCheckedChange,
        onSelectOrUnselectAll = viewModel::toggleSelected,
        onSelectDone = onSelectDone
    )
}

@Composable
private fun FilterByCollectionContent(
    collections: List<CoinCollectionAddition> = emptyList(),
    previousSelectedCollectionIds: Set<Int> = emptySet(),
    selectedCollectionIds: Set<Int> = emptySet(),
    onCheckedChange: ((Int, Boolean) -> Unit) = { _, _ -> },
    onSelectDone: () -> Unit = {},
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val isSelectAll = selectedCollectionIds.size == collections.size
    NunchukTheme {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.background)
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    IconButton(modifier = Modifier.align(Alignment.CenterStart), onClick = {
                        onBackPressedDispatcher?.onBackPressed()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back icon"
                        )
                    }
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        text = stringResource(R.string.nc_collections),
                        style = NunchukTheme.typography.titleLarge
                    )
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .clickable { onSelectOrUnselectAll(isSelectAll) },
                        text = if (isSelectAll) stringResource(R.string.nc_unselect_all) else stringResource(
                            R.string.nc_select_all
                        ),
                        style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(collections) { collectionAddition ->
                        CollectionItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            id = collectionAddition.collection.id,
                            name = collectionAddition.collection.name,
                            numCoins = collectionAddition.numCoins,
                            checked = selectedCollectionIds.contains(collectionAddition.collection.id),
                            onCheckedChange = { isSelect ->
                                onCheckedChange(collectionAddition.collection.id, isSelect)
                            })
                    }
                }

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onSelectDone,
                    enabled = selectedCollectionIds != previousSelectedCollectionIds
                ) {
                    Text(text = stringResource(R.string.nc_apply))
                }
            }
        }
    }
}

@Preview
@Composable
private fun FilterByTagScreenPreview() {
    FilterByCollectionContent(

    )
}