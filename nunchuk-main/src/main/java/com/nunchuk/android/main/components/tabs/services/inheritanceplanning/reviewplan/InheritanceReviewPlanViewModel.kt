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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.core.domain.membership.*
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Period
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.user.SetSetupInheritanceUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceReviewPlanViewModel @Inject constructor(
    private val getAssistedWalletIdsFlowUseCase: GetAssistedWalletIdFlowUseCase,
    private val calculateRequiredSignaturesInheritanceUseCase: CalculateRequiredSignaturesInheritanceUseCase,
    private val getInheritanceUserDataUseCase: GetInheritanceUserDataUseCase,
    private val cancelInheritanceUserDataUseCase: CancelInheritanceUserDataUseCase,
    private val createOrUpdateInheritanceUseCase: CreateOrUpdateInheritanceUseCase,
    private val cancelInheritanceUseCase: CancelInheritanceUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val setSetupInheritanceUseCase: SetSetupInheritanceUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = InheritanceReviewPlanFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceReviewPlanEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceReviewPlanState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    init {
        updateDataState()
        getWalletName()
    }

    private fun getWalletName() = viewModelScope.launch {
        val walletId = getAssistedWalletIdsFlowUseCase(Unit).firstOrNull()?.getOrNull() ?: return@launch
        getWalletUseCase.execute(walletId)
            .flowOn(Dispatchers.IO)
            .onException { _event.emit(InheritanceReviewPlanEvent.ProcessFailure(it.message.orUnknownError())) }
            .flowOn(Dispatchers.Main)
            .collect { wallet ->
                _state.update { state ->
                    state.copy(
                        walletId = walletId,
                        walletName = wallet.wallet.name
                    )
                }
            }
    }

    fun calculateRequiredSignatures(isCreateOrUpdateFlow: Boolean) = viewModelScope.launch {
        val stateValue = _state.value
        val walletId = stateValue.walletId ?: return@launch
        _event.emit(InheritanceReviewPlanEvent.Loading(true))
        val resultCalculate = calculateRequiredSignaturesInheritanceUseCase(
            CalculateRequiredSignaturesInheritanceUseCase.Param(
                walletId = walletId,
                note = stateValue.note,
                notificationEmails = stateValue.emails.toList(),
                notifyToday = stateValue.isNotifyToday,
                activationTimeMilis = stateValue.activationDate,
                bufferPeriodId = stateValue.bufferPeriod?.id,
                isCancelInheritance = isCreateOrUpdateFlow.not()
            )
        )
        val resultUserData = if (isCreateOrUpdateFlow.not()) {
            cancelInheritanceUserDataUseCase(CancelInheritanceUserDataUseCase.Param(walletId = walletId))
        } else {
            getInheritanceUserDataUseCase(
                GetInheritanceUserDataUseCase.Param(
                    walletId = walletId,
                    note = stateValue.note,
                    notificationEmails = stateValue.emails.toList(),
                    notifyToday = stateValue.isNotifyToday,
                    activationTimeMilis = stateValue.activationDate,
                    bufferPeriodId = stateValue.bufferPeriod?.id
                )
            )
        }
        val userData = resultUserData.getOrThrow()
        _state.update {
            it.copy(
                userData = userData,
                walletId = walletId,
                isCreateOrUpdateFlow = isCreateOrUpdateFlow
            )
        }
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (resultCalculate.isSuccess) {
            _event.emit(
                InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess(
                    type = resultCalculate.getOrThrow().type,
                    walletId = walletId,
                    userData = userData,
                    requiredSignatures = resultCalculate.getOrThrow().requiredSignatures
                )
            )
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private fun updateDataState() {
        _state.update {
            it.copy(
                activationDate = args.activationDate, note = args.note,
                isNotifyToday = args.isNotify, emails = args.emails.toList(),
                bufferPeriod = args.bufferPeriod
            )
        }
    }

    fun updateActivationDate(time: Long) = viewModelScope.launch {
        _state.update { it.copy(activationDate = time) }
    }

    fun updateNote(note: String) = viewModelScope.launch {
        _state.update { it.copy(note = note) }
    }

    fun updateNotifyPref(isNotify: Boolean, emails: List<String>) = viewModelScope.launch {
        _state.update { it.copy(isNotifyToday = isNotify, emails = emails) }
    }

    fun updateBufferPeriod(period: Period?) = viewModelScope.launch {
        _state.update { it.copy(bufferPeriod = period) }
    }

    fun handleFlow(
        signatures: HashMap<String, String>,
        securityQuestionToken: String
    ) {
        if (_state.value.isCreateOrUpdateFlow) {
            createOrUpdateInheritance(signatures, securityQuestionToken)
        } else {
            cancelInheritance(signatures, securityQuestionToken)
        }
    }

    private fun createOrUpdateInheritance(
        signatures: HashMap<String, String>,
        securityQuestionToken: String
    ) = viewModelScope.launch {
        val state = _state.value
        _event.emit(InheritanceReviewPlanEvent.Loading(true))
        val isUpdate = args.planFlow == InheritancePlanFlow.VIEW
        val result = createOrUpdateInheritanceUseCase(
            CreateOrUpdateInheritanceUseCase.Param(
                signatures = signatures,
                verifyToken = args.verifyToken,
                userData = state.userData.orEmpty(),
                securityQuestionToken = securityQuestionToken,
                isUpdate = isUpdate,
                plan = membershipStepManager.plan
            )
        )
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(isDataChanged = true) }
            setSetupInheritanceUseCase(true)
            _event.emit(InheritanceReviewPlanEvent.CreateOrUpdateInheritanceSuccess)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun isDataChanged() = _state.value.isDataChanged

    private fun cancelInheritance(
        signatures: HashMap<String, String>,
        securityQuestionToken: String
    ) = viewModelScope.launch {
        val state = _state.value
        _event.emit(InheritanceReviewPlanEvent.Loading(true))
        val result = cancelInheritanceUseCase(
            CancelInheritanceUseCase.Param(
                signatures = signatures,
                verifyToken = args.verifyToken,
                userData = state.userData.orEmpty(),
                securityQuestionToken = securityQuestionToken,
                walletId = state.walletId.orEmpty()
            )
        )
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(isDataChanged = true) }
            _event.emit(InheritanceReviewPlanEvent.CancelInheritanceSuccess)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

}