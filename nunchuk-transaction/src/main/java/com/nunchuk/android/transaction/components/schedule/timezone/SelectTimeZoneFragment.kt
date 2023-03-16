/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.transaction.components.schedule.timezone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.transaction.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectTimeZoneFragment : Fragment() {
    private val viewModel: SelectTimeZoneViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                SelectTimeZoneScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when(event) {
                        is SelectTimeZoneEvent.OnSelectTimeZone -> {
                            setFragmentResult(
                                REQUEST_KEY,
                                bundleOf(EXTRA_TIME_ZONE to event.zone)
                            )
                            findNavController().popBackStack()
                        }
                    }
                }
        }
    }

    companion object {
        const val REQUEST_KEY = "SelectTimeZoneFragment"
        const val EXTRA_TIME_ZONE = "EXTRA_TIME_ZONE"
    }
}

@Composable
private fun SelectTimeZoneScreen(viewModel: SelectTimeZoneViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SelectTimeZoneContent(state.timezones, viewModel::onSearch, viewModel::onTimeZoneClicked)
}

@Composable
private fun SelectTimeZoneContent(
    timezones: List<TimeZoneDetail> = emptyList(),
    onSearch: (value: String) -> Unit = {},
    onTimeZoneClicked: (zone: TimeZoneDetail) -> Unit = {}
) {
    var search by remember { mutableStateOf(TextFieldValue("")) }
    val onBackPressDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    NunchukTheme {
        Scaffold(topBar = {
            TopAppBar(elevation = 0.dp, backgroundColor = MaterialTheme.colors.background) {
                IconButton(onClick = { onBackPressDispatcher?.onBackPressed() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back"
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.scale(scaleX = 1f, scaleY = 0.9f),
                    value = search,
                    onValueChange = {
                        search = it
                        onSearch(it.text)
                    },
                    placeholder = {
                        Text(
                            text = "Search time zone", style = NunchukTheme.typography.body.copy(
                                color = colorResource(
                                    id = R.color.nc_boulder_color
                                )
                            )
                        )
                    },
                    shape = RoundedCornerShape(44.dp),
                )
            }
        }) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(timezones) {
                    Column(modifier = Modifier.fillMaxWidth().clickable { onTimeZoneClicked(it) }) {
                        Text(text = it.city, style = NunchukTheme.typography.body)
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = it.country,
                            style = NunchukTheme.typography.bodySmall
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = it.offset,
                            style = NunchukTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SelectTimeZoneScreenPreview() {
    SelectTimeZoneContent()
}