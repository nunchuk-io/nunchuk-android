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

package com.nunchuk.android.settings

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CleanUpCryptoAssetsUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.settings.DeleteAccountEvent.*
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DeleteAccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val repository: UserProfileRepository,
    private val cleanUpCryptoAssetsUseCase: CleanUpCryptoAssetsUseCase,
    private val sessionHolder: SessionHolder,
    private val applicationScope: CoroutineScope
) : NunchukViewModel<DeleteAccountState, DeleteAccountEvent>() {

    override val initialState = DeleteAccountState("")

    init {
        updateState { copy(email = accountManager.getAccount().email) }
    }

    fun sendConfirmDeleteAccount(confirmationCode: String) {
        viewModelScope.launch {
            repository.confirmDeleteAccount(confirmationCode)
                .onStart { event(Loading) }
                .flowOn(Dispatchers.IO)
                .onException { event(ConfirmDeleteError(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { handleSuccess() }
        }
    }

    private fun handleSuccess() {
        viewModelScope.launch {
            cleanUpCryptoAssetsUseCase.execute()
                .flatMapLatest {
                    flow {
                        emit(sessionHolder.clearActiveSession())
                    }
                }
                .flatMapLatest {
                    flow {
                        emit(accountManager.clearUserData())
                    }
                }
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect {
                    event(ConfirmDeleteSuccess)
                }
        }
    }

    fun signOutNunchuk() {
        applicationScope.launch {
            repository.signOut()
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect {}
        }
    }

}
