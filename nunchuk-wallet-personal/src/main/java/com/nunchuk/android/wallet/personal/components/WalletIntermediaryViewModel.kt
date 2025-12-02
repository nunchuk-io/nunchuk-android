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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.JoinFreeGroupWalletUseCase
import com.nunchuk.android.core.domain.ParseWalletDescriptorUseCase
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.domain.membership.SetLocalMembershipPlanFlowUseCase
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.wallet.WalletOption
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.CheckGroupWalletExistUseCase
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetGlobalGroupWalletConfigUseCase
import com.nunchuk.android.usecase.ImportWalletUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetPendingGroupsSandboxUseCase
import com.nunchuk.android.usecase.membership.GetGroupAssistedWalletConfigUseCase
import com.nunchuk.android.usecase.membership.GetPersonalMembershipStepUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WalletIntermediaryViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    private val application: Application,
    private val getGroupAssistedWalletConfigUseCase: GetGroupAssistedWalletConfigUseCase,
    private val getPersonalMembershipStepUseCase: GetPersonalMembershipStepUseCase,
    private val setLocalMembershipPlanFlowUseCase: SetLocalMembershipPlanFlowUseCase,
    private val joinFreeGroupWalletUseCase: JoinFreeGroupWalletUseCase,
    private val getPendingGroupsSandboxUseCase: GetPendingGroupsSandboxUseCase,
    private val importWalletUseCase: ImportWalletUseCase,
    private val getGlobalGroupWalletConfigUseCase: GetGlobalGroupWalletConfigUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val checkGroupWalletExistUseCase: CheckGroupWalletExistUseCase,
    private val parseWalletDescriptorUseCase: ParseWalletDescriptorUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletIntermediaryState())
    val state = _state.asStateFlow()
    private val _event = MutableSharedFlow<WalletIntermediaryEvent>()
    val event = _event.asSharedFlow()

    private var getWalletConfigJob: Job? = null

    fun init(isQuickWallet: Boolean) {
        viewModelScope.launch {
            getCompoundSignersUseCase.execute().collect { signers ->
                val signerSize = signers.first.size + signers.second.size
                val isHasSigner = signerSize > if (isQuickWallet && signers.first.any {
                        it.isVisible && it.name.startsWith("Inherited key")
                    }) 1 else 0
                _state.update { it.copy(isHasSigner = isHasSigner) }
                if (!isHasSigner) {
                    _event.emit(WalletIntermediaryEvent.NoSigner)
                }
            }
        }
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { plans ->
                    _state.update { it.copy(isMembership = plans.isNotEmpty()) }
                    if (plans.isNotEmpty()) {
                        getAssistedWalletConfig()
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
        viewModelScope.launch {
            getPendingGroupsSandboxUseCase(Unit).onSuccess { groupSandbox ->
                _state.update {
                    it.copy(numOfFreeGroupWallet = groupSandbox.size)
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

    fun extractFilePath(uri: Uri, isGroupWallet: Boolean = false) {
        viewModelScope.launch {
            _event.emit(WalletIntermediaryEvent.Loading(true))
            val result = withContext(ioDispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)
            }
            _event.emit(WalletIntermediaryEvent.Loading(false))
            _event.emit(
                WalletIntermediaryEvent.OnLoadFileSuccess(
                    uri = uri,
                    path = result?.absolutePath.orEmpty(),
                    isGroupWallet = isGroupWallet
                )
            )
        }
    }

    fun getGroupStage(): MembershipStage {
        if (state.value.personalSteps.isEmpty()) return MembershipStage.NONE
        return MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
    }

    fun isPersonalWalletAvailable(): Boolean {
        val walletsCount = state.value.walletsCount
        return state.value.personalOptions.sumOf { walletsCount[it.slug] ?: 0 } > 0
    }

    fun isGroupWalletAvailable(): Boolean {
        val walletsCount = state.value.walletsCount
        return state.value.groupOptions.sumOf { walletsCount[it.slug] ?: 0 } > 0
    }

    fun getPersonalWalletCount(): Int {
        val walletsCount = state.value.walletsCount
        return state.value.personalOptions.sumOf { walletsCount[it.slug] ?: 0 }
    }

    fun getGroupWalletCount(): Int {
        val walletsCount = state.value.walletsCount
        return state.value.groupOptions.sumOf { walletsCount[it.slug] ?: 0 }
    }

    fun setLocalMembershipPlan(plan: MembershipPlan) {
        viewModelScope.launch {
            setLocalMembershipPlanFlowUseCase(plan)
        }
    }

    fun checkRemainingGroupWalletLimit(onAction: (Boolean) -> Unit) = viewModelScope.launch {
        getGlobalGroupWalletConfigUseCase(AddressType.NATIVE_SEGWIT).onSuccess {
            onAction(it.remain <= 0)
        }
    }

    fun handleInputWalletLink(link: String) {
        viewModelScope.launch {
            _event.emit(WalletIntermediaryEvent.Loading(true))
            joinFreeGroupWalletUseCase(link).onSuccess {
                _event.emit(WalletIntermediaryEvent.Loading(false))
                _event.emit(WalletIntermediaryEvent.JoinGroupWalletSuccess(it.id))
            }.onFailure {
                _event.emit(WalletIntermediaryEvent.Loading(false))
                _event.emit(WalletIntermediaryEvent.JoinGroupWalletFailed)
            }
        }
    }

    private fun importWallet(filePath: String, name: String, description: String) {
        viewModelScope.launch {
            importWalletUseCase.execute(filePath, name, description)
                .flowOn(Dispatchers.IO)
                .onException { _event.emit(WalletIntermediaryEvent.ShowError(it.readableMessage())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    checkGroupWallet(wallet = it, filePath = filePath)
                }
        }
    }

    fun parseWalletDescriptor(uri: Uri, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getFileFromUri(application.contentResolver, uri, application.cacheDir)?.let {
                val content = it.readText()
                parseWalletDescriptorUseCase(content).onSuccess {
                    importWallet(filePath, it.name.ifBlank { "Group Wallet" }, it.description)
                }.onFailure {
                    _event.emit(WalletIntermediaryEvent.ShowError(it.message.orUnknownError()))
                }
            }
        }
    }

    private fun checkGroupWallet(wallet: Wallet, filePath: String) {
        viewModelScope.launch {
            checkGroupWalletExistUseCase(wallet.id).onSuccess {
                if (it) {
                    _event.emit(WalletIntermediaryEvent.ImportWalletSuccessEvent(wallet, filePath))
                } else {
                    deleteWalletUseCase.execute(wallet.id)
                    _event.emit(WalletIntermediaryEvent.ShowError("Group wallet not found"))
                }
            }
        }
    }

    val hasSigner: Boolean
        get() = _state.value.isHasSigner
}

sealed class WalletIntermediaryEvent {
    data class Loading(val isLoading: Boolean) : WalletIntermediaryEvent()
    data class OnLoadFileSuccess(val uri: Uri, val path: String, val isGroupWallet: Boolean) :
        WalletIntermediaryEvent()

    data class ShowError(val msg: String) : WalletIntermediaryEvent()
    data object NoSigner : WalletIntermediaryEvent()
    data class JoinGroupWalletSuccess(val groupId: String) : WalletIntermediaryEvent()
    data object JoinGroupWalletFailed : WalletIntermediaryEvent()
    data class ImportWalletSuccessEvent(val wallet: Wallet, val path: String) :
        WalletIntermediaryEvent()
}

data class WalletIntermediaryState(
    val isHasSigner: Boolean = false,
    val isMembership: Boolean = false,
    val walletsCount: Map<String, Int> = emptyMap(),
    val personalOptions: List<WalletOption> = emptyList(),
    val groupOptions: List<WalletOption> = emptyList(),
    val personalSteps: List<MembershipStepInfo> = emptyList(),
    val numOfFreeGroupWallet: Int = 0,
)

