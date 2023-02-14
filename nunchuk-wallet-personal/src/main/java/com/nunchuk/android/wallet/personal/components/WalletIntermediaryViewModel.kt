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

package com.nunchuk.android.wallet.personal.components

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.membership.GetLocalCurrentSubscriptionPlan
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val application: Application,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletIntermediaryState())
    val state = _state.asStateFlow()
    private val _event = MutableSharedFlow<WalletIntermediaryEvent>()
    val event = _event.asSharedFlow()

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
            viewModelScope.launch {
                getAssistedWalletsFlowUseCase(Unit)
                    .map { it.getOrElse { emptyList() } }
                    .collect { assistedWallets ->
                        _state.update { it.copy(assistedWallets = assistedWallets) }
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
        if (state.value.assistedWallets.any { it.isSetupInheritance.not() && it.plan == MembershipPlan.HONEY_BADGER }) return MembershipStage.SETUP_INHERITANCE
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
    val assistedWallets: List<AssistedWalletBrief> = emptyList(),
)

