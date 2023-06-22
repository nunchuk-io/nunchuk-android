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

package com.nunchuk.android.signer.software.components.primarykey.intro.replace

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PKeyReplaceKeyIntroViewModel @Inject constructor(
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder
) : NunchukViewModel<Unit, PKeyReplaceKeyIntroEvent>() {
    override val initialState: Unit = Unit

    fun checkNeedPassphraseSent() {
        setEvent(PKeyReplaceKeyIntroEvent.LoadingEvent(true))
        viewModelScope.launch {
            val isNeeded = primaryKeySignerInfoHolder.isNeedPassphraseSent()
            setEvent(PKeyReplaceKeyIntroEvent.LoadingEvent(false))
            setEvent(PKeyReplaceKeyIntroEvent.CheckNeedPassphraseSent(isNeeded))
        }
    }
}