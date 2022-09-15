package com.nunchuk.android.main.membership.key.recoveryquestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.ConfigSecurityQuestionUseCase
import com.nunchuk.android.core.domain.membership.CreateSecurityQuestionUseCase
import com.nunchuk.android.core.domain.membership.GetSecurityQuestionUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.model.SecurityQuestionModel
import com.nunchuk.android.model.QuestionsAndAnswer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryQuestionViewModel @Inject constructor(
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val configSecurityQuestionUseCase: ConfigSecurityQuestionUseCase,
    private val createSecurityQuestionUseCase: CreateSecurityQuestionUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<RecoveryQuestionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(RecoveryQuestionState.Empty)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(300L)
            _event.emit(RecoveryQuestionEvent.Loading(true))
            val result = getSecurityQuestionUseCase(Unit)
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

    fun getSecurityQuestionList(index: Int) = viewModelScope.launch {
        val questions = state.value.securityQuestions
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

    fun onContinueClicked() = viewModelScope.launch {
        _event.emit(RecoveryQuestionEvent.Loading(true))
        val questionsAndAnswers = state.value.recoveries.map {
            if (it.question.id == SecurityQuestionModel.CUSTOM_QUESTION_ID) {
                val result = createSecurityQuestionUseCase(it.question.customQuestion.orEmpty())
                if (result.isSuccess) {
                    QuestionsAndAnswer(answer = it.answer, questionId = result.getOrThrow().id)
                } else {
                    _event.emit(RecoveryQuestionEvent.Loading(false))
                    _event.emit(RecoveryQuestionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                    return@launch
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
        val result = configSecurityQuestionUseCase(questionsAndAnswers)
        _event.emit(RecoveryQuestionEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(RecoveryQuestionEvent.ConfigRecoveryQuestionSuccess)
        } else {
            _event.emit(RecoveryQuestionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

}