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

package com.nunchuk.android.utils

object CoinTagColorUtil {

    private val coinTagColors = arrayListOf<CoinTagColor>()
    val hexColors = arrayListOf<String>()

    init {
        hexColors.apply {
            add("#9EC063")
            add("#2F466C")
            add("#FAA077")
            add("#1C652D")
            add("#B4DCFF")
            add("#7E519B")
            add("#FDD95C")
            add("#595959")
            add("#D38FFF")
            add("#CF4018")
            add("#FFFFFF")
            add("#A66800")
        }
        getCoinTagColors()
    }

    private fun getCoinTagColors() {
        hexColors.forEachIndexed { index, color ->
            coinTagColors.add(CoinTagColor(index, color))
        }
    }
}

data class CoinTagColor(val index: Int, val value: String)
