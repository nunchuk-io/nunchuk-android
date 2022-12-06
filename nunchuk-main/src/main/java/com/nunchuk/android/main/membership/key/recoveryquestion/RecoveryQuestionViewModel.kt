package com.nunchuk.android.main.membership.key.recoveryquestion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.core.domain.membership.*
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.model.SecurityQuestionModel
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryQuestionViewModel @Inject constructor(
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val configSecurityQuestionUseCase: ConfigSecurityQuestionUseCase,
    private val createSecurityQuestionUseCase: CreateSecurityQuestionUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val calculateRequiredSignaturesSecurityQuestionUseCase: CalculateRequiredSignaturesSecurityQuestionUseCase,
    private val getAssistedWalletIdsFlowUseCase: GetAssistedWalletIdFlowUseCase,
    private val getSecurityQuestionsUserDataUseCase: GetSecurityQuestionsUserDataUseCase,
    private val securityQuestionsUpdateUseCase: SecurityQuestionsUpdateUseCase,
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
                    verifyToken = args.verifyToken
                )
            )
            _event.emit(RecoveryQuestionEvent.Loading(false))
            if (result.isSuccess) {
                val questions = result.getOrThrow()
                    .map { SecurityQuestionModel(id = it.id, question = it.question) }
                _state.update {
                    it.copy(securityQuestions = questions)
                }
            }
        }
    }

    fun onDiscardChangeClick() = viewModelScope.launch {
        _event.emit(RecoveryQuestionEvent.DiscardChangeClick)
    }

    fun getSecurityQuestionList(index: Int) = viewModelScope.launch {
        val value = _state.value
        val selectedQuestionSet = hashSetOf<String>()
        value.recoveries.forEach {
            selectedQuestionSet.add(it.question.id)
        }
        val questions = value.securityQuestions.filter {
            selectedQuestionSet.contains(it.id).not()
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
        val newRecovery = newRecoveries[index].copy(answer = answer)
        newRecoveries[index] = newRecovery
        _state.update {
            it.copy(recoveries = newRecoveries)
        }
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
        getAssistedWalletIdsFlowUseCase(Unit).collect { it ->
            val walletId = it.getOrNull() ?: return@collect
            val questionsAndAnswers = getQuestionsAndAnswers()
            if (questionsAndAnswers.isEmpty()) return@collect
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
            _event.emit(RecoveryQuestionEvent.Loading(false))
            if (resultCalculate.isSuccess) {
                _state.update { it.copy(clearFocusRequest = true) }
                _event.emit(
                    RecoveryQuestionEvent.CalculateRequiredSignaturesSuccess(
                        walletId,
                        userData,
                        resultCalculate.getOrThrow().requiredSignatures,
                        resultCalculate.getOrThrow().type,
                    )
                )
            } else {
                _event.emit(RecoveryQuestionEvent.ShowError(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun securityQuestionUpdate(signatures: HashMap<String, String>) = viewModelScope.launch {
        val state = _state.value
        _event.emit(RecoveryQuestionEvent.Loading(true))
        val result = securityQuestionsUpdateUseCase(
            SecurityQuestionsUpdateUseCase.Param(
                signatures = signatures,
                verifyToken = args.verifyToken,
                userData = state.userData.orEmpty()
            )
        )
        _event.emit(RecoveryQuestionEvent.Loading(false))
        if (result.isSuccess) {
            val newRecoveries = state.recoveries.map {
                it.copy(answer = "", isShowMask = true)
            }
            _state.update {
                it.copy(recoveries = newRecoveries, clearFocusRequest = false)
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
                membershipStepManager.plan
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
                    QuestionsAndAnswer(answer = it.answer, questionId = result.getOrThrow().id)
                } else {
                    _event.emit(RecoveryQuestionEvent.Loading(false))
                    _event.emit(RecoveryQuestionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                    return emptyList()
                }
            } else {
                QuestionsAndAnswer(answer = it.answer, questionId = it.question.id)
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

}