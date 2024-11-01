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

package com.nunchuk.android.settings.localcurrency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.settings.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocalCurrencyFragment : Fragment() {

    private val viewModel: LocalCurrencyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LocalCurrencyScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is LocalCurrencyEvent.Error -> showError(message = event.message)
                LocalCurrencyEvent.SetLocalCurrencySuccess -> {
                    NcToastManager.scheduleShowMessage(message = getString(R.string.nc_local_currency_updated))
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
fun LocalCurrencyScreen(
    viewModel: LocalCurrencyViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LocalCurrencyContent(currencies = state.currencies,
        selectedCurrency = state.selectedCurrency,
        currentCurrency = state.currentCurrency,
        onSelectCurrency = {
            viewModel.selectCurrency(it)
        }, onSaveClick = {
            viewModel.onSaveClick()
        })
}

@Composable
private fun LocalCurrencyContent(
    currencies: LinkedHashMap<String, String> = linkedMapOf(),
    selectedCurrency: String = "",
    currentCurrency: String = "",
    onSelectCurrency: (String) -> Unit = {},
    onSaveClick: () -> Unit = { },
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_local_currency),
                    textStyle = NunchukTheme.typography.titleLarge
                )
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    currencies.forEach { (unit, name) ->
                        item {
                            CurrencyItem(
                                name = name,
                                unit = unit,
                                selected = selectedCurrency == unit,
                                onSelectCurrency = {
                                    onSelectCurrency(unit)
                                }
                            )
                        }
                    }
                }
                NcPrimaryDarkButton(
                    enabled = currentCurrency.isNotBlank() && currentCurrency != selectedCurrency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onSaveClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_save))
                }
            }
        }
    }
}

@Composable
private fun CurrencyItem(
    name: String,
    unit: String,
    selected: Boolean,
    onSelectCurrency: () -> Unit = {}
) {
    val backgroundColor = if (selected) colorResource(id = R.color.nc_grey_light) else MaterialTheme.colorScheme.background
    Row(
        modifier = Modifier
            .background(color = backgroundColor)
            .clickable {
                onSelectCurrency()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "$name ($unit)", style = NunchukTheme.typography.body,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(painter = painterResource(id = R.drawable.ic_check), contentDescription = "")
        }
    }
}

@Preview
@Composable
private fun LocalCurrencyScreenPreview() {
    val map = HashMap<String, String>()
    map["MXN"] = "Fijian Dollar"
    map["STD"] = "Congolese Franc"
    map["CDF"] = "Chilean Peso"
    // Create a List of Map.Entry objects from the HashMap
    val list = ArrayList(map.entries)

    // Sort the List using a Comparator that compares the values of the Map.Entry objects using compareTo()
    list.sortWith(compareBy { it.value })

    // Create a new LinkedHashMap and add the sorted Map.Entry objects to it
    val sortedMap = LinkedHashMap<String, String>()
    for (entry in list) {
        sortedMap[entry.key] = entry.value
    }
    LocalCurrencyContent(currencies = sortedMap)
}