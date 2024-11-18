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

import android.os.Parcelable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.android.parcel.Parcelize
import javax.inject.Inject

@HiltViewModel
class CoinFilterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CoinFilterFragmentArgs =
        CoinFilterFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val selectTags = mutableStateOf(args.filter.selectTags)
    val selectCollections = mutableStateOf(args.filter.selectCollections)
    val startTime = mutableStateOf(args.filter.startTime)
    val endTime = mutableStateOf(args.filter.endTime)

    fun setSelectedTags(tagIds: IntArray) {
        selectTags.value = tagIds.toSet()
    }

    fun setSelectedCollection(collectionIds: IntArray) {
        selectCollections.value = collectionIds.toSet()
    }

    fun setDate(isStart: Boolean, timeInMillis: Long) {
        if (isStart) {
            startTime.value = timeInMillis
        } else {
            endTime.value = timeInMillis
        }
    }
}

@Parcelize
data class CoinFilterUiState(
    val selectTags: Set<Int> = emptySet(),
    val selectCollections: Set<Int> = emptySet(),
    val startTime: Long = -1,
    val endTime: Long = -1,
    val min: String = "",
    val isMinBtc: Boolean = false,
    val max: String = "",
    val isMaxBtc: Boolean = false,
    val showLockedCoin: Boolean = true,
    val showUnlockedCoin: Boolean = true,
    val isDescending: Boolean = true,
) : Parcelable