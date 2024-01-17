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

package com.nunchuk.android.settings

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.domain.DeletePrimaryKeyUseCase
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetSyncSettingUseCase
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.settings.AccountSettingEvent.CheckNeedPassphraseSent
import com.nunchuk.android.settings.AccountSettingEvent.DeletePrimaryKeySuccess
import com.nunchuk.android.settings.AccountSettingEvent.Loading
import com.nunchuk.android.settings.AccountSettingEvent.RequestDeleteError
import com.nunchuk.android.settings.AccountSettingEvent.RequestDeleteSuccess
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AccountSettingViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val deletePrimaryKeyUseCase: DeletePrimaryKeyUseCase,
    private val appScope: CoroutineScope,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getSyncSettingUseCase: GetSyncSettingUseCase,
) : NunchukViewModel<AccountSettingState, AccountSettingEvent>() {

    override val initialState = AccountSettingState()

    init {
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit)
                .combine(getSyncSettingUseCase(Unit)) { wallets, isSyncEnable ->
                    wallets to isSyncEnable
                }
                .collect {
                    updateState {
                        copy(
                            hasAssistedWallets = it.first.getOrElse { emptyList() }.isNotEmpty(),
                            isSyncEnable = it.second.getOrElse { false }
                        )
                    }
                }
        }
    }

    fun sendRequestDeleteAccount() {
        viewModelScope.launch {
            repository.requestDeleteAccount()
                .onStart { event(Loading) }
                .flowOn(Dispatchers.IO)
                .onException { event(RequestDeleteError(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { event(RequestDeleteSuccess) }
        }
    }

    fun deletePrimaryKey(passphrase: String) = viewModelScope.launch {
        setEvent(Loading)
        val result = deletePrimaryKeyUseCase(DeletePrimaryKeyUseCase.Param(passphrase))
        if (result.isFailure) {
            setEvent(RequestDeleteError(result.exceptionOrNull()?.message.orUnknownError()))
            return@launch
        }
        if (result.isSuccess) {
            appScope.launch(dispatcher) {
                clearInfoSessionUseCase.invoke(Unit)
                event(DeletePrimaryKeySuccess)
            }
        }
    }

    fun checkNeedPassphraseSent() {
        setEvent(Loading)
        viewModelScope.launch {
            val isNeeded = primaryKeySignerInfoHolder.isNeedPassphraseSent()
            setEvent(CheckNeedPassphraseSent(isNeeded))
        }
    }

}
