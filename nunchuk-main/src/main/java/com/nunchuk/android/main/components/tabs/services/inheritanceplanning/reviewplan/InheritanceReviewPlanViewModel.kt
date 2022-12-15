package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesInheritanceUseCase
import com.nunchuk.android.core.domain.membership.CreateUpdateInheritanceUpdateUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceUserDataUseCase
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.orUnknownError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceReviewPlanViewModel @Inject constructor(
    private val getAssistedWalletIdsFlowUseCase: GetAssistedWalletIdFlowUseCase,
    private val calculateRequiredSignaturesInheritanceUseCase: CalculateRequiredSignaturesInheritanceUseCase,
    private val getInheritanceUserDataUseCase: GetInheritanceUserDataUseCase,
    private val createUpdateInheritanceUpdateUseCase: CreateUpdateInheritanceUpdateUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = InheritanceReviewPlanFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceReviewPlanEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceReviewPlanState())
    val state = _state.asStateFlow()

    init {
        updateDataState()
    }

    fun calculateRequiredSignatures() = viewModelScope.launch {
        getAssistedWalletIdsFlowUseCase(Unit).collect { it ->
            val walletId = it.getOrNull() ?: return@collect
            _event.emit(InheritanceReviewPlanEvent.Loading(true))
            val stateValue = _state.value
            val resultCalculate = calculateRequiredSignaturesInheritanceUseCase(
                CalculateRequiredSignaturesInheritanceUseCase.Param(
                    walletId = walletId,
                    note = args.note,
                    notificationEmails = args.emails.toList(),
                    notifyToday = args.isNotify,
                    activationTimeMilis = args.activationDate
                )
            )
            val resultUserData = getInheritanceUserDataUseCase(
                GetInheritanceUserDataUseCase.Param(
                    walletId = walletId,
                    note = args.note,
                    notificationEmails = args.emails.toList(),
                    notifyToday = args.isNotify,
                    activationTimeMilis = args.activationDate
                )
            )
            val userData = resultUserData.getOrThrow()
            _state.update {
                it.copy(userData = userData)
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
    }

    private fun updateDataState() {
        _state.update {
            it.copy(
                activationDate = args.activationDate, note = args.note,
                isNotifyToday = args.isNotify, emails = args.emails.toList()
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

    fun createUpdateInheritance(signatures: HashMap<String, String>, securityQuestionToken: String) =
        viewModelScope.launch {
            val state = _state.value
            _event.emit(InheritanceReviewPlanEvent.Loading(true))
            val isUpdate = args.planFlow == InheritancePlanFlow.VIEW
            val result = createUpdateInheritanceUpdateUseCase(
                CreateUpdateInheritanceUpdateUseCase.Param(
                    signatures = signatures,
                    verifyToken = args.verifyToken,
                    userData = state.userData.orEmpty(),
                    securityQuestionToken = securityQuestionToken,
                    isUpdate = isUpdate
                )
            )
            _event.emit(InheritanceReviewPlanEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(InheritanceReviewPlanEvent.CreateInheritanceSuccess)
            } else {
                _event.emit(InheritanceReviewPlanEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }

}