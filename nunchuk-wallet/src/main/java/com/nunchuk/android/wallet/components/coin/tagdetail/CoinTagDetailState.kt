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

package com.nunchuk.android.wallet.components.coin.tagdetail

import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput

data class CoinTagDetailState(
    val coinTag: CoinTag? = null,
    val coins: List<UnspentOutput> = emptyList(),
    val tags: Map<Int, CoinTag> = emptyMap(),
)

sealed class CoinTagDetailEvent {
    data class Loading(val show: Boolean) : CoinTagDetailEvent()
    data class Error(val message: String) : CoinTagDetailEvent()
    object DeleteTagSuccess : CoinTagDetailEvent()
    object UpdateTagColorSuccess : CoinTagDetailEvent()
    object RemoveCoinSuccess : CoinTagDetailEvent()
}
