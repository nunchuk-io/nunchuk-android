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

package com.nunchuk.android.wallet.personal.components

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.membership.GetAssistedWalletConfigUseCase
import com.nunchuk.android.usecase.membership.GetLocalCurrentSubscriptionPlan
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WalletIntermediaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCompoundSignersUseCase: Lazy<GetCompoundSignersUseCase>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getLocalCurrentSubscriptionPlan: GetLocalCurrentSubscriptionPlan,
    private val membershipStepManager: MembershipStepManager,
    private val application: Application,
    private val getAssistedWalletConfigUseCase: GetAssistedWalletConfigUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(WalletIntermediaryState())
    val state = _state.asStateFlow()
    private val _event = MutableSharedFlow<WalletIntermediaryEvent>()
    val event = _event.asSharedFlow()

    private var getWalletConfigJob: Job? = null

    init {
        val args = WalletIntermediaryFragmentArgs.fromSavedStateHandle(savedStateHandle)
        if (args.isQuickWallet) {
            viewModelScope.launch {
                getCompoundSignersUseCase.get().execute().collect {
                    _state.value =
                        WalletIntermediaryState(isHasSigner = it.first.isNotEmpty() || it.second.isNotEmpty())
                }
            }
        } else {
            viewModelScope.launch {
                getLocalCurrentSubscriptionPlan(Unit)
                    .map { it.getOrElse { MembershipPlan.NONE } }
                    .collect { plan ->
                        _state.update { it.copy(plan = plan) }
                    }
            }
        }
        getAssistedWalletConfig()
    }

    fun getAssistedWalletConfig() {
        if (getWalletConfigJob?.isActive == true) return
        getWalletConfigJob = viewModelScope.launch {
            getAssistedWalletConfigUseCase(Unit).onSuccess { configs ->
                _state.update { it.copy(remainWalletCount = configs.remainingWalletCount) }
            }
        }
    }

    fun extractFilePath(uri: Uri) {
        viewModelScope.launch {
            _event.emit(WalletIntermediaryEvent.Loading(true))
            val result = withContext(ioDispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)
            }
            _event.emit(WalletIntermediaryEvent.Loading(false))
            _event.emit(WalletIntermediaryEvent.OnLoadFileSuccess(result?.absolutePath.orEmpty()))
        }
    }

    fun getGroupStage(): MembershipStage {
        if (membershipStepManager.isNotConfig()) return MembershipStage.NONE
        return MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
    }

    val hasSigner: Boolean
        get() = _state.value.isHasSigner
}

sealed class WalletIntermediaryEvent {
    data class Loading(val isLoading: Boolean) : WalletIntermediaryEvent()
    data class OnLoadFileSuccess(val path: String) : WalletIntermediaryEvent()
    data class ShowError(val msg: String) : WalletIntermediaryEvent()
}

data class WalletIntermediaryState(
    val isHasSigner: Boolean = false,
    val plan: MembershipPlan = MembershipPlan.NONE,
    val remainWalletCount: Int = 0,
)

