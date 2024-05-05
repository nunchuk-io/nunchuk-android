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

package com.nunchuk.android.signer.tapsigner.backup.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.backup.verify.model.TsBackUpOption
import com.nunchuk.android.signer.tapsigner.backup.verify.model.TsBackUpOptionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerVerifyBackUpOptionViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<TapSignerVerifyBackUpOptionEvent>()
    val event = _event.asSharedFlow()

    private val _options = MutableStateFlow(OPTIONS)
    val options = _options.asStateFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(OnContinueClicked)
        }
    }

    fun onOptionClicked(option: TsBackUpOption) {
        val options =
            _options.value.toList().map { it.copy(isSelected = it.labelId == option.labelId) }
        _options.value = options
    }

    val selectedOptionType: TsBackUpOptionType
        get() = _options.value.first { it.isSelected }.type

    companion object {
        val OPTIONS = listOf(
            TsBackUpOption(
                type = TsBackUpOptionType.BY_MYSELF,
                labelId = R.string.nc_verify_backup_myself,
                isSelected = true
            ),
            TsBackUpOption(
                type = TsBackUpOptionType.BY_APP,
                labelId = R.string.nc_verify_backup_via_the_app,
            ),
            TsBackUpOption(
                type = TsBackUpOptionType.SKIP,
                labelId = R.string.nc_skip_verification
            )
        )
    }
}

sealed class TapSignerVerifyBackUpOptionEvent

object OnContinueClicked : TapSignerVerifyBackUpOptionEvent()
