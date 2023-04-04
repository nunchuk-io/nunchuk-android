package com.nunchuk.android.wallet.components.coin.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.CurrencyFormatter
import com.nunchuk.android.core.util.MAX_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.filter.collection.FilterByCollectionFragment
import com.nunchuk.android.wallet.components.coin.filter.collection.FilterByCollectionFragmentArgs
import com.nunchuk.android.wallet.components.coin.filter.tag.FilterByTagFragment
import com.nunchuk.android.wallet.components.coin.filter.tag.FilterByTagFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinFilterFragment : Fragment() {
    private val viewModel: CoinFilterViewModel by navGraphViewModels(R.id.coin_search_navigation)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinFilterScreen(
                    viewModel,
                    onOpenSelectTagScreen = {
                        findNavController().navigate(
                            CoinFilterFragmentDirections.actionCoinFilterFragmentToFilterByTagFragment(
                                viewModel.state.value.selectTags.toIntArray()
                            )
                        )
                    },
                    onOpenSelectCollectionScreen = {
                        findNavController().navigate(
                            CoinFilterFragmentDirections.actionCoinFilterFragmentToFilterByCollectionFragment(
                                viewModel.state.value.selectCollections.toIntArray()
                            )
                        )
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(FilterByTagFragment.REQUEST_KEY) { _, bundle ->
            val args = FilterByTagFragmentArgs.fromBundle(bundle)
            viewModel.setSelectedTags(args.tagIds)
        }

        setFragmentResultListener(FilterByCollectionFragment.REQUEST_KEY) { _, bundle ->
            val args = FilterByCollectionFragmentArgs.fromBundle(bundle)
            viewModel.setSelectedCollection(args.collectionIds)
        }
    }
}

@Composable
private fun CoinFilterScreen(
    viewModel: CoinFilterViewModel = viewModel(),
    onOpenSelectTagScreen: () -> Unit,
    onOpenSelectCollectionScreen: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CoinFilterContent(
        state = state,
        onOpenSelectTagScreen = onOpenSelectTagScreen,
        onOpenSelectCollectionScreen = onOpenSelectCollectionScreen,
    )
}

@Composable
private fun CoinFilterContent(
    state: CoinFilterUiState = CoinFilterUiState(),
    onApplyFilter: () -> Unit = {},
    onSwitchBtcAndCurrency: (Boolean) -> Unit = {},
    onShowLockedCoin: (Boolean) -> Unit = {},
    onShowUnlockedCoin: (Boolean) -> Unit = {},
    onSelectSort: (Boolean) -> Unit = {},
    onOpenSelectTagScreen: () -> Unit = {},
    onOpenSelectCollectionScreen: () -> Unit = {},
    filters: List<CoinFilter> = listOf(
        CoinFilter.Date(),
        CoinFilter.LockCoin(),
        CoinFilter.Sort()
    ),
) {
    var min by rememberSaveable {
        mutableStateOf("")
    }
    var max by rememberSaveable {
        mutableStateOf("")
    }
    NunchukTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
            ) {
                LazyColumn(modifier = Modifier.weight(1.0f)) {
                    item {
                        FilterRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            title = stringResource(id = R.string.nc_tags),
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .clickable(onClick = onOpenSelectTagScreen)
                                    .border(
                                        width = 1.dp,
                                        shape = RoundedCornerShape(8.dp),
                                        color = NcColor.border,
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterStart),
                                    text = if (state.selectTags.isEmpty()) stringResource(R.string.nc_all_tags)
                                    else stringResource(
                                        R.string.nc_tags_selected, state.selectTags.size
                                    ),
                                    style = NunchukTheme.typography.body
                                )
                                Icon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.CenterEnd),
                                    painter = painterResource(id = R.drawable.ic_arrow_expand),
                                    contentDescription = "Arrow Expand"
                                )
                            }
                        }
                    }
                    item {
                        FilterRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            title = stringResource(id = R.string.nc_collections),
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .clickable(onClick = onOpenSelectCollectionScreen)
                                    .border(
                                        width = 1.dp,
                                        shape = RoundedCornerShape(8.dp),
                                        color = NcColor.border,
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterStart),
                                    text = if (state.selectCollections.isEmpty())
                                        stringResource(R.string.nc_all_collections)
                                    else stringResource(
                                        R.string.nc_collections_selected,
                                        state.selectCollections.size
                                    ),
                                    style = NunchukTheme.typography.body
                                )
                                Icon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.CenterEnd),
                                    painter = painterResource(id = R.drawable.ic_arrow_expand),
                                    contentDescription = "Arrow Expand"
                                )
                            }
                        }
                    }
                    item {
                        FilterRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            title = stringResource(id = R.string.nc_amount),
                        ) {
                            NcTextField(modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                title = stringResource(R.string.nc_minimum_amount),
                                value = min,
                                onValueChange = { value: String ->
                                    min = CurrencyFormatter.format(value, MAX_FRACTION_DIGITS)
                                },
                                visualTransformation = NumberCommaTransformation(),
                                rightContent = {
                                    SwitchAmount(true, onSwitchBtcAndCurrency)
                                })
                            NcTextField(modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                title = stringResource(R.string.nc_maximum_amount),
                                value = max,
                                onValueChange = { value: String -> max = CurrencyFormatter.format(value) },
                                rightContent = {
                                    SwitchAmount(false, onSwitchBtcAndCurrency)
                                })
                        }
                    }
                    filters.forEach { filter ->
                        when (filter) {
                            is CoinFilter.Date -> item {
                                FilterRow(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = stringResource(id = R.string.nc_date),
                                ) {
                                    Text(
                                        modifier = Modifier.padding(top = 16.dp),
                                        text = stringResource(R.string.nc_from),
                                        style = NunchukTheme.typography.titleSmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .border(
                                                width = 1.dp,
                                                shape = RoundedCornerShape(8.dp),
                                                color = NcColor.border
                                            )
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        text = "mm/dd/yyyy",
                                        style = NunchukTheme.typography.body,
                                    )
                                    Text(
                                        modifier = Modifier.padding(top = 16.dp),
                                        text = stringResource(R.string.nc_to),
                                        style = NunchukTheme.typography.titleSmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .border(
                                                width = 1.dp,
                                                shape = RoundedCornerShape(8.dp),
                                                color = NcColor.border
                                            )
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        text = "mm/dd/yyyy",
                                        style = NunchukTheme.typography.body,
                                    )
                                }
                            }
                            is CoinFilter.LockCoin -> item {
                                FilterRow(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = stringResource(R.string.nc_include_locked_coins),
                                ) {
                                    Box(modifier = Modifier.padding(top = 16.dp)) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(alignment = Alignment.CenterStart),
                                            text = "Show locked coins",
                                            style = NunchukTheme.typography.body
                                        )
                                        Checkbox(
                                            modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                            checked = filter.showLockedCoin,
                                            onCheckedChange = onShowLockedCoin
                                        )
                                    }

                                    Box(modifier = Modifier.padding(top = 16.dp)) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(alignment = Alignment.CenterStart),
                                            text = "Show unlocked coins",
                                            style = NunchukTheme.typography.body
                                        )
                                        Checkbox(
                                            modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                            checked = filter.showLockedCoin,
                                            onCheckedChange = onShowUnlockedCoin
                                        )
                                    }
                                }
                            }
                            is CoinFilter.Sort -> item {
                                FilterRow(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = stringResource(R.string.nc_sort),
                                ) {
                                    Box(modifier = Modifier.padding(top = 16.dp)) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(alignment = Alignment.CenterStart),
                                            text = "Sort in descending order",
                                            style = NunchukTheme.typography.body
                                        )
                                        RadioButton(modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                            selected = filter.isAscending.not(),
                                            onClick = { onSelectSort(false) })
                                    }

                                    Box(modifier = Modifier.padding(top = 16.dp)) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(alignment = Alignment.CenterStart),
                                            text = "Show unlocked coins",
                                            style = NunchukTheme.typography.body
                                        )
                                        RadioButton(modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                            selected = filter.isAscending,
                                            onClick = { onSelectSort(true) })
                                    }
                                }
                            }
                        }
                    }
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onApplyFilter,
                ) {
                    Text(text = stringResource(R.string.nc_apply))
                }
            }
        }
    }
}

@Composable
private fun SwitchAmount(isMin: Boolean, onSwitchBtcAndCurrency: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.clickable { onSwitchBtcAndCurrency(isMin) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_switch),
            contentDescription = "Switch",
            tint = MaterialTheme.colors.primary
        )
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(id = R.string.nc_transaction_switch_to_usd),
            textDecoration = TextDecoration.Underline,
            style = NunchukTheme.typography.titleSmall
        )
    }
}

@Composable
fun FilterRow(modifier: Modifier, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.padding(top = 24.dp)) {
        Text(text = title, style = NunchukTheme.typography.title)
        content()
        Divider(modifier = Modifier.padding(top = 24.dp))
    }
}

class NumberCommaTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatValue = text.text.toDoubleOrNull()?.formatDecimalWithoutZero() ?: ""
        val value = if (text.text.endsWith(".")) "${formatValue}." else formatValue
        return TransformedText(
            text = AnnotatedString(value),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return value.length
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return text.length
                }
            }
        )
    }
}

@Preview
@Composable
private fun CoinFilterScreenPreview() {
    CoinFilterContent(

    )
}