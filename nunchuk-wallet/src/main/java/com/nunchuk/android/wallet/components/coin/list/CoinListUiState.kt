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

package com.nunchuk.android.wallet.components.coin.list

import com.nunchuk.android.compose.miniscript.TimelockInfo
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput

data class CoinListUiState(
    val mode: CoinListMode = CoinListMode.NONE,
    val coins: List<UnspentOutput> = emptyList(),
    val tags: Map<Int, CoinTag> = emptyMap(),
    val collections: Map<Int, CoinCollection> = emptyMap(),
    val selectedCoins: Set<UnspentOutput> = setOf(),
    val spendableAmount: Amount = Amount(0L),
    val timelockInfo: TimelockInfo? = null,
)