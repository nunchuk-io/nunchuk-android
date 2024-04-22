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
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.wallet.WalletOption
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.membership.GetGroupAssistedWalletConfigUseCase
import com.nunchuk.android.usecase.membership.GetPersonalMembershipStepUseCase
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WalletIntermediaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCompoundSignersUseCase: Lazy<GetCompoundSignersUseCase>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    private val application: Application,
    private val getGroupAssistedWalletConfigUseCase: GetGroupAssistedWalletConfigUseCase,
    private val getPersonalMembershipStepUseCase: GetPersonalMembershipStepUseCase,
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
                getCompoundSignersUseCase.get().execute().collect { signers ->
                    _state.update { it.copy(isHasSigner = signers.first.isNotEmpty() || signers.second.isNotEmpty()) }
                }
            }
        } else {
            viewModelScope.launch {
                getLocalMembershipPlansFlowUseCase(Unit)
                    .map { it.getOrElse { emptyList() } }
                    .collect { plans ->
                        if (plans.isNotEmpty()) {
                            getAssistedWalletConfig()
                        }
                    }
            }
        }
        viewModelScope.launch {
            getPersonalMembershipStepUseCase(Unit).map { result ->
                result.getOrElse { emptyList() }
            }.collect { steps ->
                _state.update {
                    it.copy(personalSteps = steps)
                }
            }
        }
    }

    fun getAssistedWalletConfig() {
        if (getWalletConfigJob?.isActive == true) return
        getWalletConfigJob = viewModelScope.launch {
            getGroupAssistedWalletConfigUseCase(Unit).onSuccess { configs ->
                _state.update {
                    it.copy(
                        walletsCount = configs.walletsCount,
                        personalOptions = configs.personalOptions,
                        groupOptions = configs.groupOptions
                    )
                }
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
        if (state.value.personalSteps.isEmpty()) return MembershipStage.NONE
        return MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
    }

    fun isPersonalWalletAvailable() : Boolean {
        val walletsCount = state.value.walletsCount
        return state.value.personalOptions.sumOf { walletsCount[it.slug] ?: 0 } > 0
    }

    fun isGroupWalletAvailable() : Boolean {
        val walletsCount = state.value.walletsCount
        return state.value.groupOptions.sumOf { walletsCount[it.slug] ?: 0 } > 0
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
    val walletsCount: Map<String, Int> = emptyMap(),
    val personalOptions: List<WalletOption> = emptyList(),
    val groupOptions: List<WalletOption> = emptyList(),
    val personalSteps: List<MembershipStepInfo> = emptyList(),
)

