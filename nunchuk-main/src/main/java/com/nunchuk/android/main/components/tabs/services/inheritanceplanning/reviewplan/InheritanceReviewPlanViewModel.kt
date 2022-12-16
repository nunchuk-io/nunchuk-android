package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.core.domain.membership.*
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.GetWalletUseCase
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = InheritanceReviewPlanFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceReviewPlanEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceReviewPlanState())
    val state = _state.asStateFlow()

    init {
        updateDataState()
        getWalletName()
    }

    private fun getWalletName() = viewModelScope.launch {
        getAssistedWalletIdsFlowUseCase(Unit).collect { it ->
            val walletId = it.getOrNull() ?: return@collect
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
    }

    fun calculateRequiredSignatures(isCreateOrUpdate: Boolean) = viewModelScope.launch {
        getAssistedWalletIdsFlowUseCase(Unit).collect { it ->
            val stateValue = _state.value
            val walletId = it.getOrNull() ?: return@collect
            _event.emit(InheritanceReviewPlanEvent.Loading(true))
            val resultCalculate = calculateRequiredSignaturesInheritanceUseCase(
                CalculateRequiredSignaturesInheritanceUseCase.Param(
                    walletId = walletId,
                    note = stateValue.note,
                    notificationEmails = stateValue.emails.toList(),
                    notifyToday = stateValue.isNotifyToday,
                    activationTimeMilis = stateValue.activationDate
                )
            )
            val resultUserData = if (isCreateOrUpdate) {
                getInheritanceUserDataUseCase(
                    GetInheritanceUserDataUseCase.Param(
                        walletId = walletId,
                        note = stateValue.note,
                        notificationEmails = stateValue.emails.toList(),
                        notifyToday = stateValue.isNotifyToday,
                        activationTimeMilis = stateValue.activationDate
                    )
                )
            } else {
                cancelInheritanceUserDataUseCase(CancelInheritanceUserDataUseCase.Param(walletId = walletId))
            }
            val userData = resultUserData.getOrThrow()
            _state.update {
                it.copy(
                    userData = userData,
                    walletId = walletId,
                    isCreateOrUpdateFlow = isCreateOrUpdate
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
    }

    private fun updateDataState() {
        _state.update {
            it.copy(
                activationDate = 1671466534000, note = args.note,
                isNotifyToday = args.isNotify, emails = args.emails.toList()
            )
        }
    }

    fun updateActivationDate(time: Long) = viewModelScope.launch {
        _state.update { it.copy(activationDate = 1671466534000) }
    }

    fun updateNote(note: String) = viewModelScope.launch {
        _state.update { it.copy(note = note) }
    }

    fun updateNotifyPref(isNotify: Boolean, emails: List<String>) = viewModelScope.launch {
        _state.update { it.copy(isNotifyToday = isNotify, emails = emails) }
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
                isUpdate = isUpdate
            )
        )
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(InheritanceReviewPlanEvent.CreateOrUpdateInheritanceSuccess)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

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
            _event.emit(InheritanceReviewPlanEvent.CancelInheritanceSuccess)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

}