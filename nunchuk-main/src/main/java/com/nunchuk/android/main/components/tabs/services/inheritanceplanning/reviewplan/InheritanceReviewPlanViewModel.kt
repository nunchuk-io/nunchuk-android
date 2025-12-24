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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesInheritanceUseCase
import com.nunchuk.android.core.domain.membership.CancelInheritanceUseCase
import com.nunchuk.android.core.domain.membership.CancelInheritanceUserDataUseCase
import com.nunchuk.android.core.domain.membership.CreateOrUpdateInheritanceUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceUserDataUseCase
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.CalculateRequiredSignaturesAction
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupRemoteUseCase
import com.nunchuk.android.usecase.membership.MarkSetupInheritanceUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceReviewPlanViewModel @Inject constructor(
    private val calculateRequiredSignaturesInheritanceUseCase: CalculateRequiredSignaturesInheritanceUseCase,
    private val getInheritanceUserDataUseCase: GetInheritanceUserDataUseCase,
    private val cancelInheritanceUserDataUseCase: CancelInheritanceUserDataUseCase,
    private val createOrUpdateInheritanceUseCase: CreateOrUpdateInheritanceUseCase,
    private val cancelInheritanceUseCase: CancelInheritanceUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getGroupRemoteUseCase: GetGroupRemoteUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val markSetupInheritanceUseCase: MarkSetupInheritanceUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private lateinit var param: InheritancePlanningParam.SetupOrReview
    private var initialParam: InheritancePlanningParam.SetupOrReview? = null

    internal val reviewFlow: ReviewFlow?
        get() = savedStateHandle.get<ReviewFlow>(EXTRA_REVIEW_FLOW)

    private val _event = MutableSharedFlow<InheritanceReviewPlanEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceReviewPlanState())
    val state = _state.asStateFlow()

    private val _isContinueButtonEnabled = MutableStateFlow(true)
    val isContinueButtonEnabled = _isContinueButtonEnabled.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    fun init(param: InheritancePlanningParam.SetupOrReview) {
        this.param = param
        initialParam = param.copy()
        getWalletName()
        if (param.groupId.isNotEmpty()) {
            getGroup()
        }
    }

    fun update(param: InheritancePlanningParam.SetupOrReview) {
        this.param = param
        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        val shouldEnable = param.planFlow == InheritancePlanFlow.SETUP ||
                          (param.planFlow == InheritancePlanFlow.VIEW && hasDataChanged())
        _isContinueButtonEnabled.value = shouldEnable
    }

    private fun hasDataChanged(): Boolean {
        val initial = initialParam ?: return true

        return initial.activationDate != param.activationDate ||
                initial.selectedZoneId != param.selectedZoneId ||
                initial.emails != param.emails ||
                initial.isNotify != param.isNotify ||
                initial.note != param.note ||
                initial.bufferPeriod != param.bufferPeriod ||
                !notificationSettingsEqual(initial.notificationSettings, param.notificationSettings)
    }

    private fun notificationSettingsEqual(
        first: InheritanceNotificationSettings?,
        second: InheritanceNotificationSettings?
    ): Boolean {
        if (first == null && second == null) return true
        if (first == null || second == null) return false
        
        return first.emailMeWalletConfig == second.emailMeWalletConfig &&
                first.perEmailSettings == second.perEmailSettings
    }

    private fun getGroup() {
        viewModelScope.launch {
            getGroupRemoteUseCase(GetGroupRemoteUseCase.Params(param.groupId)).onSuccess { group ->
                val currentUserRole = byzantineGroupUtils.getCurrentUserRole(group)
                _state.update {
                    it.copy(currentUserRole = currentUserRole)
                }
            }
        }
    }

    private fun getWalletName() = viewModelScope.launch {
        getWalletUseCase.execute(param.walletId)
            .flowOn(Dispatchers.IO)
            .onException { _event.emit(InheritanceReviewPlanEvent.ProcessFailure(it.message.orUnknownError())) }
            .flowOn(Dispatchers.Main)
            .collect { wallet ->
                _state.update { state ->
                    state.copy(
                        walletId = param.walletId,
                        walletName = wallet.wallet.name
                    )
                }
            }
    }

    fun calculateRequiredSignatures(flow: ReviewFlow) = viewModelScope.launch {
        savedStateHandle[EXTRA_REVIEW_FLOW] = flow
        val walletId = param.walletId
        _event.emit(InheritanceReviewPlanEvent.Loading(true))
        val resultCalculate = calculateRequiredSignaturesInheritanceUseCase(
            CalculateRequiredSignaturesInheritanceUseCase.Param(
                walletId = walletId,
                note = param.note,
                notificationEmails = param.emails.toList(),
                notifyToday = param.isNotify,
                activationTimeMilis = param.activationDate,
                bufferPeriodId = param.bufferPeriod?.id,
                action = if (flow == ReviewFlow.CREATE_OR_UPDATE) CalculateRequiredSignaturesAction.CREATE_OR_UPDATE else CalculateRequiredSignaturesAction.CANCEL,
                groupId = param.groupId,
                notificationPreferences = param.notificationSettings,
                timezone = param.selectedZoneId
            )
        )
        val userData = getUserData()
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (resultCalculate.isSuccess) {
            handleCalculateRequiredSignatures(resultCalculate.getOrThrow(), userData)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private suspend fun handleCalculateRequiredSignatures(
        signatures: CalculateRequiredSignatures,
        userData: String
    ) {
        if (reviewFlow == null) return
        if (signatures.type == VerificationType.SIGN_DUMMY_TX) {
            if (reviewFlow == ReviewFlow.CREATE_OR_UPDATE) {
                createOrUpdateInheritanceUseCase(
                    CreateOrUpdateInheritanceUseCase.Param(
                        signatures = hashMapOf(),
                        verifyToken = param.verifyToken,
                        userData = userData,
                        securityQuestionToken = "",
                        isUpdate = param.planFlow == InheritancePlanFlow.VIEW,
                        draft = true
                    )
                ).onSuccess { transactionId ->
                    _state.update {
                        it.copy(
                            dummyTransactionId = transactionId,
                            requiredSignature = signatures
                        )
                    }
                    _event.emit(
                        InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess(
                            type = signatures.type,
                            walletId = param.walletId,
                            userData = userData,
                            requiredSignatures = signatures.requiredSignatures,
                            dummyTransactionId = transactionId
                        )
                    )
                }.onFailure {
                    _event.emit(InheritanceReviewPlanEvent.ProcessFailure(it.message.orUnknownError()))
                }
            } else {
                cancelInheritanceUseCase(
                    CancelInheritanceUseCase.Param(
                        signatures = hashMapOf(),
                        verifyToken = param.verifyToken,
                        userData = userData,
                        securityQuestionToken = "",
                        walletId = param.walletId,
                        draft = true
                    )
                ).onSuccess { transactionId ->
                    _state.update {
                        it.copy(
                            dummyTransactionId = transactionId,
                            requiredSignature = signatures
                        )
                    }
                    _event.emit(
                        InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess(
                            type = signatures.type,
                            walletId = param.walletId,
                            userData = userData,
                            requiredSignatures = signatures.requiredSignatures,
                            dummyTransactionId = transactionId
                        )
                    )
                }.onFailure {
                    _event.emit(InheritanceReviewPlanEvent.ProcessFailure(it.message.orUnknownError()))
                }
            }
        } else if (signatures.type == VerificationType.SECURITY_QUESTION) {
            _event.emit(
                InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess(
                    type = signatures.type,
                    walletId = param.walletId,
                    userData = userData,
                    requiredSignatures = signatures.requiredSignatures,
                    dummyTransactionId = ""
                )
            )
        }
    }

    private suspend fun getUserData(): String {
        val resultUserData = if (reviewFlow == ReviewFlow.CANCEL) {
            cancelInheritanceUserDataUseCase(
                CancelInheritanceUserDataUseCase.Param(
                    walletId = param.walletId,
                    groupId = param.groupId
                )
            )
        } else {
            getInheritanceUserDataUseCase(
                GetInheritanceUserDataUseCase.Param(
                    walletId = param.walletId,
                    note = param.note,
                    notificationEmails = param.emails.toList(),
                    notifyToday = param.isNotify,
                    activationTimeMilis = param.activationDate,
                    bufferPeriodId = param.bufferPeriod?.id,
                    groupId = param.groupId,
                    notificationPreferences = param.notificationSettings,
                    timezone = param.selectedZoneId
                )
            )
        }
        val userData = resultUserData.getOrThrow()
        _state.update {
            it.copy(
                userData = userData,
                walletId = param.walletId
            )
        }
        return userData
    }

    fun handleFlow(
        signatures: HashMap<String, String>,
        securityQuestionToken: String
    ) {
        if (reviewFlow == null) return
        if (reviewFlow == ReviewFlow.CREATE_OR_UPDATE) {
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
        val isUpdate = param.planFlow == InheritancePlanFlow.VIEW
        val result = createOrUpdateInheritanceUseCase(
            CreateOrUpdateInheritanceUseCase.Param(
                signatures = signatures,
                verifyToken = param.verifyToken,
                userData = state.userData.orEmpty(),
                securityQuestionToken = securityQuestionToken,
                isUpdate = isUpdate,
                draft = false
            )
        )
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(isDataChanged = true) }
            markSetupInheritance()
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
                verifyToken = param.verifyToken,
                userData = state.userData.orEmpty(),
                securityQuestionToken = securityQuestionToken,
                walletId = param.walletId,
                draft = false
            )
        )
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(isDataChanged = true) }
            markSetupInheritance()
            _event.emit(InheritanceReviewPlanEvent.CancelInheritanceSuccess)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun markSetupInheritance() = viewModelScope.launch {
        markSetupInheritanceUseCase(
            MarkSetupInheritanceUseCase.Param(
                walletId = param.walletId,
                isSetupInheritance = reviewFlow == ReviewFlow.CREATE_OR_UPDATE
            )
        )
        _event.emit(InheritanceReviewPlanEvent.MarkSetupInheritance)
    }

    enum class ReviewFlow {
        CREATE_OR_UPDATE, CANCEL
    }

    companion object {
        private const val EXTRA_REVIEW_FLOW = "review_flow"
    }
}