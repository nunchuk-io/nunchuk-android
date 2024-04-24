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

package com.nunchuk.android.main.membership.key.recoveryquestion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesSecurityQuestionUseCase
import com.nunchuk.android.core.domain.membership.ConfigSecurityQuestionUseCase
import com.nunchuk.android.core.domain.membership.CreateSecurityQuestionUseCase
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.domain.membership.GetSecurityQuestionUseCase
import com.nunchuk.android.core.domain.membership.GetSecurityQuestionsUserDataUseCase
import com.nunchuk.android.core.domain.membership.SecurityQuestionsUpdateUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.model.SecurityQuestionModel
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.model.SecurityQuestion
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.containsPersonalPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryQuestionViewModel @Inject constructor(
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val configSecurityQuestionUseCase: ConfigSecurityQuestionUseCase,
    private val createSecurityQuestionUseCase: CreateSecurityQuestionUseCase,
    private val calculateRequiredSignaturesSecurityQuestionUseCase: CalculateRequiredSignaturesSecurityQuestionUseCase,
    private val getSecurityQuestionsUserDataUseCase: GetSecurityQuestionsUserDataUseCase,
    private val securityQuestionsUpdateUseCase: SecurityQuestionsUpdateUseCase,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = RecoveryQuestionFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<RecoveryQuestionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(RecoveryQuestionState.Empty)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(300L)
            _event.emit(RecoveryQuestionEvent.Loading(true))
            val result = getSecurityQuestionUseCase(
                GetSecurityQuestionUseCase.Param(
                    isFilterAnswer = false,
                )
            )
            _event.emit(RecoveryQuestionEvent.Loading(false))
            if (result.isSuccess) {
                val questions = result.getOrThrow()
                val questionModels = questions
                    .map { SecurityQuestionModel(id = it.id, question = it.question) }
                _state.update {
                    it.copy(
                        securityQuestions = questionModels,
                        recoveries = recoveryListInitialize(questions)
                    )
                }
            }
        }
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { plans ->
                    _state.update { it.copy(plans = plans) }
                }
        }
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { assistedWallets ->
                    _state.update { it.copy(assistedWallets = assistedWallets) }
                }
        }
    }

    private fun recoveryListInitialize(questions: List<SecurityQuestion> = emptyList()): List<RecoveryData> {
        val recoveryList = mutableListOf<RecoveryData>()
        if (args.isRecoveryFlow) {
            val answeredQuestions = questions.filter { it.isAnswer }
            if (answeredQuestions.size < 3) {
                (0..2).forEach {
                    recoveryList.add(RecoveryData(index = it))
                }
            } else {
                (0..2).forEach {
                    val question = answeredQuestions[it]
                    recoveryList.add(
                        RecoveryData(
                            index = it,
                            question = SecurityQuestionModel(
                                id = question.id,
                                question = question.question
                            ),
                            isShowMask = true
                        )
                    )
                }
            }
        } else {
            (0..2).forEach {
                recoveryList.add(RecoveryData(index = it))
            }
        }
        return recoveryList
    }

    fun onDiscardChangeClick() = viewModelScope.launch {
        _event.emit(RecoveryQuestionEvent.DiscardChangeClick)
    }

    fun getSecurityQuestionList(index: Int) = viewModelScope.launch {
        val value = _state.value
        val selectedQuestionSet = hashSetOf<String>()
        value.recoveries.forEach {
            val question = if (it.question.question.isNullOrBlank()
                    .not()
            ) it.question.question else it.question.customQuestion
            selectedQuestionSet.add(question.orEmpty())
        }
        val questions = value.securityQuestions.filter {
            selectedQuestionSet.contains(it.question).not()
        }
        if (questions.isNotEmpty()) {
            _state.update {
                it.copy(interactQuestionIndex = index)
            }
            _event.emit(RecoveryQuestionEvent.GetSecurityQuestionSuccess(questions))
        } else {
            _event.emit(RecoveryQuestionEvent.ShowError("Can not get security question, Please check network connection"))
        }
    }

    fun updateAnswer(index: Int, answer: String) {
        val newRecoveries = state.value.recoveries.toMutableList()
        val newRecovery = newRecoveries[index].copy(answer = answer, change = answer.isNotBlank())
        newRecoveries[index] = newRecovery
        _state.update {
            it.copy(recoveries = newRecoveries)
        }
    }

    fun updateClearFocus(focus: Boolean) {
        _state.update { it.copy(clearFocusRequest = focus) }
    }

    fun updateCustomQuestion(index: Int, question: String) {
        val newRecoveries = state.value.recoveries.toMutableList()
        val recoveryQuestion = newRecoveries[index]
        val newRecovery =
            recoveryQuestion.copy(question = recoveryQuestion.question.copy(customQuestion = question))
        newRecoveries[index] = newRecovery
        _state.update {
            it.copy(recoveries = newRecoveries)
        }
    }

    fun updateQuestion(question: SecurityQuestionModel) {
        val interactQuestionIndex = state.value.interactQuestionIndex
        if (interactQuestionIndex == RecoveryQuestionState.InitValue) return
        val newRecoveries = state.value.recoveries.toMutableList()
        val newRecovery = newRecoveries[interactQuestionIndex].copy(
            question = question,
        )
        newRecoveries[interactQuestionIndex] = newRecovery
        _state.update {
            it.copy(
                recoveries = newRecoveries,
                interactQuestionIndex = RecoveryQuestionState.InitValue
            )
        }
    }

    fun updateMaskAnswer(index: Int) {
        val newRecoveries = state.value.recoveries.toMutableList()
        if (newRecoveries[index].isShowMask.not() || args.isRecoveryFlow.not()) return
        val newRecovery = newRecoveries[index].copy(isShowMask = false)
        newRecoveries[index] = newRecovery
        _state.update {
            it.copy(recoveries = newRecoveries)
        }
    }

    private fun calculateRequiredSignatures() = viewModelScope.launch {
        val walletId = getWalletId() ?: return@launch
        val questionsAndAnswers = getQuestionsAndAnswers()
        if (questionsAndAnswers.isEmpty()) return@launch
        _event.emit(RecoveryQuestionEvent.Loading(true))
        val resultCalculate = calculateRequiredSignaturesSecurityQuestionUseCase(
            CalculateRequiredSignaturesSecurityQuestionUseCase.Param(
                walletId = walletId,
                questionsAndAnswers
            )
        )
        val resultUserData = getSecurityQuestionsUserDataUseCase(
            GetSecurityQuestionsUserDataUseCase.Param(
                walletId = walletId,
                questionsAndAnswers
            )
        )
        val userData = resultUserData.getOrThrow()
        _state.update {
            it.copy(userData = userData)
        }
        val groupId = assistedWalletManager.getGroupId(walletId).orEmpty()
        _event.emit(RecoveryQuestionEvent.Loading(false))
        if (resultCalculate.isSuccess) {
            val signatures = resultCalculate.getOrThrow()
            if (signatures.type == VerificationType.SIGN_DUMMY_TX) {
                _event.emit(RecoveryQuestionEvent.Loading(true))
                securityQuestionsUpdateUseCase(
                    SecurityQuestionsUpdateUseCase.Param(
                        signatures = emptyMap(),
                        verifyToken = args.verifyToken,
                        userData = userData,
                        securityQuestionToken = "",
                        confirmCodeNonce = "",
                        confirmCodeToken = "",
                        draft = true
                    )
                ).onSuccess { transactionId ->
                    _event.emit(
                        RecoveryQuestionEvent.CalculateRequiredSignaturesSuccess(
                            walletId = walletId,
                            userData = userData,
                            requiredSignatures = resultCalculate.getOrThrow().requiredSignatures,
                            type = resultCalculate.getOrThrow().type,
                            dummyTransactionId = transactionId,
                            groupId = groupId
                        )
                    )
                }.onFailure {
                    _event.emit(RecoveryQuestionEvent.ShowError(it.message.orUnknownError()))
                }
                _event.emit(RecoveryQuestionEvent.Loading(false))
            } else {
                _event.emit(
                    RecoveryQuestionEvent.CalculateRequiredSignaturesSuccess(
                        walletId = walletId,
                        userData = userData,
                        requiredSignatures = resultCalculate.getOrThrow().requiredSignatures,
                        type = resultCalculate.getOrThrow().type,
                        dummyTransactionId = "",
                        groupId = groupId
                    )
                )
            }
        } else {
            _event.emit(RecoveryQuestionEvent.ShowError(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun securityQuestionUpdate(
        signatures: HashMap<String, String>,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String
    ) =
        viewModelScope.launch {
            val state = _state.value
            _event.emit(RecoveryQuestionEvent.Loading(true))
            val result = securityQuestionsUpdateUseCase(
                SecurityQuestionsUpdateUseCase.Param(
                    signatures = signatures,
                    verifyToken = args.verifyToken,
                    userData = state.userData.orEmpty(),
                    securityQuestionToken = securityQuestionToken,
                    confirmCodeNonce = confirmCodeNonce,
                    confirmCodeToken = confirmCodeToken,
                    draft = false
                )
            )
            _event.emit(RecoveryQuestionEvent.Loading(false))
            if (result.isSuccess) {
                val newRecoveries = state.recoveries.map {
                    it.copy(answer = "", isShowMask = true)
                }
                _state.update {
                    it.copy(recoveries = newRecoveries)
                }
                _event.emit(RecoveryQuestionEvent.RecoveryQuestionUpdateSuccess)
            } else {
                _event.emit(RecoveryQuestionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }

    fun onContinueClicked() = viewModelScope.launch {
        if (args.isRecoveryFlow) {
            calculateRequiredSignatures()
        } else {
            configSecurityQuestion()
        }
    }

    private fun configSecurityQuestion() = viewModelScope.launch {
        _event.emit(RecoveryQuestionEvent.Loading(true))
        val questionsAndAnswers = getQuestionsAndAnswers()
        if (questionsAndAnswers.isEmpty()) return@launch
        val result = configSecurityQuestionUseCase(
            ConfigSecurityQuestionUseCase.Param(
                questionsAndAnswers,
            )
        )
        _event.emit(RecoveryQuestionEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(RecoveryQuestionEvent.ConfigRecoveryQuestionSuccess)
        } else {
            _event.emit(RecoveryQuestionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private suspend fun getQuestionsAndAnswers(): List<QuestionsAndAnswer> {
        val questionsAndAnswers = state.value.recoveries.map {
            if (it.question.id == SecurityQuestionModel.CUSTOM_QUESTION_ID) {
                val result = createSecurityQuestionUseCase(it.question.customQuestion.orEmpty())
                if (result.isSuccess) {
                    QuestionsAndAnswer(
                        answer = it.answer,
                        questionId = result.getOrThrow().id,
                        change = it.change
                    )
                } else {
                    _event.emit(RecoveryQuestionEvent.Loading(false))
                    _event.emit(RecoveryQuestionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                    return emptyList()
                }
            } else {
                QuestionsAndAnswer(
                    answer = it.answer,
                    questionId = it.question.id,
                    change = it.change
                )
            }
        }
        val newRecoveryQuestions = state.value.recoveries.mapIndexed { index, recoveryData ->
            recoveryData.copy(
                question = recoveryData.question.copy(id = questionsAndAnswers[index].questionId)
            )
        }
        _state.update {
            it.copy(recoveries = newRecoveryQuestions)
        }
        return questionsAndAnswers
    }

    private fun getWalletId(): String? {
        val stateValue = _state.value
        if (stateValue.plans.containsPersonalPlan()) return stateValue.assistedWallets.firstOrNull { it.groupId.isEmpty() }?.localId.orEmpty()
        return stateValue.assistedWallets.firstOrNull { it.groupId.isNotEmpty() }?.localId
    }

}