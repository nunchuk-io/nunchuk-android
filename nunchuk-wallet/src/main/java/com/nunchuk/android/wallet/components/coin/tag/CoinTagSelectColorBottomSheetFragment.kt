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

package com.nunchuk.android.wallet.components.coin.tag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.util.hexToColor
import com.nunchuk.android.wallet.R

class CoinTagSelectColorBottomSheetFragment : BaseComposeBottomSheet() {

    private val viewModel by viewModels<CoinTagSelectColorBottomSheetViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NunchukTheme {
                    CoinTagSelectColorBottomSheetScreen(viewModel, onSetClick = {
                        setFragmentResult(
                            REQUEST_KEY,
                            CoinTagSelectColorBottomSheetFragmentArgs(it).toBundle()
                        )
                        dismissAllowingStateLoss()
                    })
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "CoinTagSelectColorBottomSheetFragment"
        const val EXTRA_SELECT_COLOR = "selected_color"
    }
}


@Composable
private fun CoinTagSelectColorBottomSheetScreen(
    viewModel: CoinTagSelectColorBottomSheetViewModel,
    onSetClick: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CoinTagSelectColorBottomSheetScreenContent(
        colors = CoinTagColorUtil.hexColors,
        selectedColor = state.selectedColor,
        onSetClick = {
            onSetClick(viewModel.getSelectedColor())
        },
        onSelectColorClick = {
            viewModel.updateSelectedColor(it)
        }
    )
}

@Composable
private fun CoinTagSelectColorBottomSheetScreenContent(
    colors: List<String> = emptyList(),
    selectedColor: String = "",
    onSetClick: () -> Unit = {},
    onSelectColorClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.nc_select_a_color),
            style = NunchukTheme.typography.title
        )
        var index = 0
        Column {
            repeat(3) {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items(4) {
                        val color = colors[index]
                        val selected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(48.dp, 48.dp)
                                .border(
                                    width = if (selected) 6.dp else 2.dp,
                                    color = if (selected) Color(0xFF031F2B) else Color(0xFFDEDEDE),
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .background(color.hexToColor())
                                .clickable {
                                    onSelectColorClick(color)
                                }
                        )
                        index++
                    }
                }
            }
        }

        NcPrimaryDarkButton(
            enabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            onClick = onSetClick,
        ) {
            Text(text = stringResource(id = R.string.nc_set_color))
        }
    }
}

@Preview
@Composable
fun CoinTagSelectColorBottomSheetScreenContentPreview() {
    NunchukTheme {
        CoinTagSelectColorBottomSheetScreenContent(
            colors = CoinTagColorUtil.hexColors,
            selectedColor = CoinTagColorUtil.hexColors.first()
        )
    }
}
