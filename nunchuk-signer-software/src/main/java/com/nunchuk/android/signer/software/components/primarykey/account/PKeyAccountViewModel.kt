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

package com.nunchuk.android.signer.software.components.primarykey.account

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.LastSignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.GetPrimaryKeyListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PKeyAccountViewModel @Inject constructor(
    private val getPrimaryKeyListUseCase: GetPrimaryKeyListUseCase,
    private val accountManager: AccountManager,
    private val lastSignInModeHolder: LastSignInModeHolder
) : NunchukViewModel<PKeyAccountState, PKeyAccountEvent>() {

    override val initialState = PKeyAccountState()

    init {
        getAccounts()
    }

    private fun getAccounts() = viewModelScope.launch {
        setEvent(PKeyAccountEvent.LoadingEvent(true))
        val result = getPrimaryKeyListUseCase(accountManager.getAccount().decoyPin.ifBlank {
            lastSignInModeHolder.getLastLoginDecoyPin()
        })
        setEvent(PKeyAccountEvent.LoadingEvent(false))
        if (result.isSuccess) {
            updateState { copy(primaryKeys = result.getOrThrow()) }
        } else {
            setEvent(PKeyAccountEvent.ProcessErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}
