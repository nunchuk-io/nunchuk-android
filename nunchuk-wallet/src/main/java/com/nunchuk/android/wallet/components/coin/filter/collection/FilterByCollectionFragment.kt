package com.nunchuk.android.wallet.components.coin.filter.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.CoinCollectionAddition
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.collection.CollectionItem
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
        selectedCoinTags = state.selectedTags,
        onCheckedChange = viewModel::onCheckedChange,
        onSelectOrUnselectAll = viewModel::toggleSelected,
        onSelectDone = onSelectDone
    )
}

@Composable
private fun FilterByCollectionContent(
    collections: List<CoinCollectionAddition> = emptyList(),
    selectedCoinTags: Set<Int> = emptySet(),
    onCheckedChange: ((Int, Boolean) -> Unit) = { _, _ -> },
    onSelectDone: () -> Unit = {},
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val isSelectAll = selectedCoinTags.size == collections.size
    NunchukTheme {
        Scaffold {
            Column(
                modifier = Modifier
                    .padding(it)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .background(color = MaterialTheme.colors.background)
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
                        text = stringResource(R.string.nc_select_coins),
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
                        CollectionItem(id = collectionAddition.collection.id,
                            name = collectionAddition.collection.name,
                            numCoins = collectionAddition.numCoins,
                            checked = selectedCoinTags.contains(collectionAddition.collection.id),
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
                    enabled = selectedCoinTags.isNotEmpty()
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