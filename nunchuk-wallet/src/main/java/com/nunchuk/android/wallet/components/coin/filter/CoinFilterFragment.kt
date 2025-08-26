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

package com.nunchuk.android.wallet.components.coin.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import com.nunchuk.android.compose.InputSwitchCurrencyView
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.util.CurrencyFormatter
import com.nunchuk.android.core.util.MAX_FRACTION_DIGITS
import com.nunchuk.android.utils.simpleGlobalDateFormat
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.filter.collection.FilterByCollectionFragment
import com.nunchuk.android.wallet.components.coin.filter.collection.FilterByCollectionFragmentArgs
import com.nunchuk.android.wallet.components.coin.filter.tag.FilterByTagFragment
import com.nunchuk.android.wallet.components.coin.filter.tag.FilterByTagFragmentArgs
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@AndroidEntryPoint
class CoinFilterFragment : Fragment() {
    private val viewModel: CoinFilterViewModel by viewModels()
    private val args: CoinFilterFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinFilterScreen(
                    isSearchTransaction = args.isSearchTransaction,
                    initValue = args.filter,
                    viewModel = viewModel,
                    onOpenSelectTagScreen = {
                        findNavController().navigate(
                            CoinFilterFragmentDirections.actionCoinFilterFragmentToFilterByTagFragment(
                                viewModel.selectTags.value.toIntArray()
                            )
                        )
                    },
                    onOpenSelectCollectionScreen = {
                        findNavController().navigate(
                            CoinFilterFragmentDirections.actionCoinFilterFragmentToFilterByCollectionFragment(
                                viewModel.selectCollections.value.toIntArray()
                            )
                        )
                    },
                    onSelectDate = { isStart ->
                        val oldCalUtc = Calendar.getInstance().apply {
                            val time =
                                if (isStart) viewModel.startTime.value else viewModel.endTime.value
                            val actualTime = if (time < 0L) System.currentTimeMillis() else time
                            timeInMillis = actualTime
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val materialDatePicker = MaterialDatePicker.Builder.datePicker()
                            .setTheme(R.style.NcMaterialCalendar)
                            .setSelection(oldCalUtc.timeInMillis)
                            .build()
                        materialDatePicker.addOnPositiveButtonClickListener { timeInMillis ->
                            viewModel.setDate(isStart, timeInMillis)
                        }
                        materialDatePicker.show(childFragmentManager, "MaterialDatePicker")
                    },
                    onApplyFilter = { min, isMinBtc, max, isMaxBtc, showLockedCoin, showUnlockedCoin, isDescending, sortByCoinAge ->
                        setFragmentResult(
                            REQUEST_KEY, CoinFilterFragmentArgs(
                                CoinFilterUiState(
                                    selectTags = viewModel.selectTags.value,
                                    selectCollections = viewModel.selectCollections.value,
                                    startTime = viewModel.startTime.value,
                                    endTime = viewModel.endTime.value,
                                    min = min,
                                    isMinBtc = isMinBtc,
                                    max = max,
                                    isMaxBtc = isMaxBtc,
                                    showLockedCoin = showLockedCoin,
                                    showUnlockedCoin = showUnlockedCoin,
                                    isDescending = isDescending,
                                    sortByCoinAge = sortByCoinAge
                                )
                            ).toBundle()
                        )
                        findNavController().popBackStack()
                    }
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

    companion object {
        const val REQUEST_KEY = "CoinFilterFragment"
    }
}

@Composable
private fun CoinFilterScreen(
    isSearchTransaction: Boolean,
    initValue: CoinFilterUiState,
    viewModel: CoinFilterViewModel = viewModel(),
    onOpenSelectTagScreen: () -> Unit,
    onOpenSelectCollectionScreen: () -> Unit,
    onApplyFilter: (min: String, isMinBtc: Boolean, max: String, isMaxBtc: Boolean, showLockedCoin: Boolean, showUnlockedCoin: Boolean, isDescending: Boolean, sortByCoinAge: Boolean) -> Unit,
    onSelectDate: (isStart: Boolean) -> Unit = {},
) {
    CoinFilterContent(
        isSearchTransaction = isSearchTransaction,
        initValue = initValue,
        selectTags = viewModel.selectTags.value,
        selectCollections = viewModel.selectCollections.value,
        startTime = viewModel.startTime.value,
        endTime = viewModel.endTime.value,
        onOpenSelectTagScreen = onOpenSelectTagScreen,
        onOpenSelectCollectionScreen = onOpenSelectCollectionScreen,
        onSelectDate = onSelectDate,
        onApplyFilter = onApplyFilter,
        onClearAll = {
            viewModel.selectTags.value = emptySet()
            viewModel.selectCollections.value = emptySet()
            viewModel.startTime.value = -1L
            viewModel.endTime.value = -1L
        }
    )
}

@Composable
private fun CoinFilterContent(
    isSearchTransaction: Boolean = false,
    initValue: CoinFilterUiState = CoinFilterUiState(),
    selectTags: Set<Int> = emptySet(),
    selectCollections: Set<Int> = emptySet(),
    startTime: Long = -1,
    endTime: Long = -1,
    onApplyFilter: (min: String, isMinBtc: Boolean, max: String, isMaxBtc: Boolean, showLockedCoin: Boolean, showUnlockedCoin: Boolean, isDescending: Boolean, sortByCoinAge: Boolean) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onOpenSelectTagScreen: () -> Unit = {},
    onOpenSelectCollectionScreen: () -> Unit = {},
    onSelectDate: (isStart: Boolean) -> Unit = {},
    onClearAll: () -> Unit = {},
) {
    var min by rememberSaveable {
        mutableStateOf(initValue.min)
    }
    var isMinBtc by rememberSaveable {
        mutableStateOf(initValue.isMinBtc)
    }
    var max by rememberSaveable {
        mutableStateOf(initValue.max)
    }
    var isMaxBtc by rememberSaveable {
        mutableStateOf(initValue.isMaxBtc)
    }
    var showLockedCoin by rememberSaveable {
        mutableStateOf(initValue.showLockedCoin)
    }
    var showUnlockedCoin by rememberSaveable {
        mutableStateOf(initValue.showUnlockedCoin)
    }
    var isDescending by rememberSaveable {
        mutableStateOf(initValue.isDescending)
    }
    var sortByCoinAge by rememberSaveable {
        mutableStateOf(initValue.sortByCoinAge)
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_filters),
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Text(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable {
                                    min = ""
                                    isMinBtc = false
                                    max = ""
                                    isMaxBtc = false
                                    showLockedCoin = true
                                    showUnlockedCoin = true
                                    isDescending = true
                                    sortByCoinAge = false
                                    onClearAll()
                                },
                            text = stringResource(R.string.nc_clear_all),
                            style = NunchukTheme.typography.title,
                            textDecoration = TextDecoration.Underline
                        )
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                Column {
                    LazyColumn(modifier = Modifier.weight(1.0f)) {
                        if (isSearchTransaction.not()) {
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
                                                color = MaterialTheme.colorScheme.strokePrimary,
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.CenterStart),
                                            text = if (selectTags.isEmpty()) stringResource(R.string.nc_no_filter)
                                            else stringResource(
                                                R.string.nc_tags_selected, selectTags.size
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
                                                color = MaterialTheme.colorScheme.strokePrimary,
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.CenterStart),
                                            text = if (selectCollections.isEmpty()) stringResource(
                                                R.string.nc_no_filter
                                            )
                                            else stringResource(
                                                R.string.nc_collections_selected,
                                                selectCollections.size
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
                        }
                        item {
                            FilterRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = stringResource(id = R.string.nc_amount),
                            ) {
                                InputSwitchCurrencyView(
                                    title = stringResource(R.string.nc_minimum_amount),
                                    isBtc = isMinBtc,
                                    currencyValue = min,
                                    onValueChange = {
                                        min = CurrencyFormatter.format(it, MAX_FRACTION_DIGITS)
                                    },
                                    onSwitchBtcAndCurrency = {
                                        isMinBtc = it
                                    }
                                )
                                InputSwitchCurrencyView(
                                    title = stringResource(R.string.nc_maximum_amount),
                                    isBtc = isMaxBtc,
                                    currencyValue = max,
                                    onValueChange = {
                                        max = CurrencyFormatter.format(it)
                                    },
                                    onSwitchBtcAndCurrency = {
                                        isMaxBtc = it
                                    }
                                )
                            }
                        }
                        item {
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
                                            color = MaterialTheme.colorScheme.strokePrimary
                                        )
                                        .clickable { onSelectDate(true) }
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    text = if (startTime <= 0L) "mm/dd/yyyy" else Date(startTime).simpleGlobalDateFormat(),
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
                                            color = MaterialTheme.colorScheme.strokePrimary
                                        )
                                        .clickable { onSelectDate(false) }
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    text = if (endTime <= 0L) "mm/dd/yyyy" else Date(endTime).simpleGlobalDateFormat(),
                                    style = NunchukTheme.typography.body,
                                )
                            }
                        }
                        if (isSearchTransaction.not()) {
                            item {
                                FilterRow(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = stringResource(R.string.nc_include_locked_coins),
                                ) {
                                    Box(modifier = Modifier.padding(top = 16.dp)) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(alignment = Alignment.CenterStart),
                                            text = stringResource(R.string.nc_show_locked_coins),
                                            style = NunchukTheme.typography.body
                                        )
                                        NcCheckBox(
                                            modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                            checked = showLockedCoin,
                                            onCheckedChange = {
                                                showLockedCoin = it
                                            })
                                    }

                                    Box(modifier = Modifier.padding(top = 16.dp)) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(alignment = Alignment.CenterStart),
                                            text = stringResource(R.string.nc_show_unlocked_coins),
                                            style = NunchukTheme.typography.body
                                        )
                                        NcCheckBox(
                                            modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                            checked = showUnlockedCoin,
                                            onCheckedChange = {
                                                showUnlockedCoin = it
                                            })
                                    }
                                }
                            }
                        }
                        item {
                            FilterRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = stringResource(R.string.nc_sort),
                            ) {
                                Box(modifier = Modifier.padding(top = 16.dp)) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(alignment = Alignment.CenterStart),
                                        text = stringResource(R.string.nc_sort_in_descending_order),
                                        style = NunchukTheme.typography.body
                                    )
                                    NcRadioButton(
                                        modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                        selected = isDescending,
                                        onClick = { isDescending = true },
                                    )
                                }

                                Box(modifier = Modifier.padding(top = 16.dp)) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(alignment = Alignment.CenterStart),
                                        text = stringResource(R.string.nc_sort_in_ascending_order),
                                        style = NunchukTheme.typography.body
                                    )
                                    NcRadioButton(
                                        modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                        selected = isDescending.not(),
                                        onClick = { isDescending = false },
                                    )
                                }
                                Box(modifier = Modifier.padding(top = 16.dp)) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(alignment = Alignment.CenterStart),
                                        text = stringResource(R.string.nc_sort_by_coin_age),
                                        style = NunchukTheme.typography.body
                                    )
                                    NcSwitch(
                                        modifier = Modifier.align(alignment = Alignment.CenterEnd),
                                        checked = sortByCoinAge,
                                        onCheckedChange = {
                                            sortByCoinAge = it
                                        },
                                    )
                                }
                            }
                        }
                    }
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = {
                            onApplyFilter(
                                min,
                                isMinBtc,
                                max,
                                isMaxBtc,
                                showLockedCoin,
                                showUnlockedCoin,
                                isDescending,
                                sortByCoinAge
                            )
                        },
                    ) {
                        Text(text = stringResource(R.string.nc_apply))
                    }
                }
            }
        }
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