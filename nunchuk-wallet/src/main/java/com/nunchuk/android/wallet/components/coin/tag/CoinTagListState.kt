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

import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition

data class CoinTagListState(
    val tags: List<CoinTagAddition> = arrayListOf(),
    val selectedCoinTags: MutableSet<Int> = hashSetOf(),
    val coinTagInputHolder: CoinTag? = null,
    val preSelectedCoinTags: MutableSet<Int> = hashSetOf()
)

sealed class CoinTagListEvent {
    data class Loading(val show: Boolean) : CoinTagListEvent()
    data class Error(val message: String) : CoinTagListEvent()
    data class AddCoinToTagSuccess(val numsCoin: Int) : CoinTagListEvent()
    object CreateTagSuccess : CoinTagListEvent()
    object ExistedTagError : CoinTagListEvent()
}

