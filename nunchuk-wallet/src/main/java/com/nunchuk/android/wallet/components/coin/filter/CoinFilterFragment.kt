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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navGraphViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.wallet.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CoinFilterFragment : BaseComposeBottomSheet() {
    private val viewModel: CoinFilterViewModel by navGraphViewModels(R.id.coin_search_navigation)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinFilterScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->

                }
        }
        dialog?.findViewById<View>(R.id.design_bottom_sheet)?.let {
            BottomSheetBehavior.from(it).isDraggable = false
        }
    }
}

@Composable
private fun CoinFilterScreen(viewModel: CoinFilterViewModel = viewModel()) {
    CoinFilterContent()
}

@Composable
private fun CoinFilterContent(
    onApplyFilter: () -> Unit = {},
    onMinimumAmountChange: (String) -> Unit = {},
    onMaximumAmountChange: (String) -> Unit = {},
    onSwitchBtcAndCurrency: (Boolean) -> Unit = {},
    onShowLockedCoin: (Boolean) -> Unit = {},
    onShowUnlockedCoin: (Boolean) -> Unit = {},
    onSelectSort: (Boolean) -> Unit = {},
    filters: List<CoinFilter> = listOf(
        CoinFilter.Tag(),
        CoinFilter.Collection(),
        CoinFilter.Amount(),
        CoinFilter.Date(),
        CoinFilter.LockCoin(),
        CoinFilter.Sort()
    ),
) {
    NunchukTheme {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .navigationBarsPadding()
        ) {
            LazyColumn(modifier = Modifier.weight(1.0f)) {
                filters.forEach { filter ->
                    when (filter) {
                        is CoinFilter.Tag -> item {
                            FilterRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = stringResource(id = R.string.nc_tags),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
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
                                        text = stringResource(R.string.nc_all_tags),
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
                        is CoinFilter.Collection -> item {
                            FilterRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = stringResource(id = R.string.nc_collections),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
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
                                        text = stringResource(R.string.nc_all_collections),
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
                        is CoinFilter.Amount -> item {
                            FilterRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = stringResource(id = R.string.nc_amount),
                            ) {
                                NcTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    title = stringResource(R.string.nc_minimum_amount),
                                    value = "",
                                    onValueChange = onMinimumAmountChange,
                                    rightContent = {
                                        SwitchAmount(true, onSwitchBtcAndCurrency)
                                    }
                                )
                                NcTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    title = stringResource(R.string.nc_maximum_amount),
                                    value = "",
                                    onValueChange = onMaximumAmountChange,
                                    rightContent = {
                                        SwitchAmount(false, onSwitchBtcAndCurrency)
                                    }
                                )
                            }
                        }
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
                                    RadioButton(
                                        modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                        selected = filter.isAscending.not(),
                                        onClick = { onSelectSort(false) }
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
                                    RadioButton(
                                        modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                        selected = filter.isAscending,
                                        onClick = { onSelectSort(true) }
                                    )
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

@Preview
@Composable
private fun CoinFilterScreenPreview() {
    CoinFilterContent(

    )
}