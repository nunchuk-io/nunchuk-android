package com.nunchuk.android.main.components.tabs.services.keyrecovery.securityquestionanswer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.DownloadBackupKeyUseCase
import com.nunchuk.android.core.domain.membership.GetSecurityQuestionUseCase
import com.nunchuk.android.core.domain.membership.VerifySecurityQuestionUseCase
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.QuestionsAndAnswer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnswerSecurityQuestionViewModel @Inject constructor(
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val downloadBackupKeyUseCase: DownloadBackupKeyUseCase,
    private val verifySecurityQuestionUseCase: VerifySecurityQuestionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = AnswerSecurityQuestionFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<AnswerSecurityQuestionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(AnswerSecurityQuestionState())
    val state = _state.asStateFlow()

    init {
        getSecurityQuestion()
    }

    fun onContinueClicked() {
        if (args.signer != null && args.verifyToken.isNotEmpty()) {
            downloadBackupKey(args.signer ?: return)
        } else {
            verifySecurityQuestion()
        }
    }

    private fun getSecurityQuestion() = viewModelScope.launch {
        val result =
            getSecurityQuestionUseCase(GetSecurityQuestionUseCase.Param(isFilterAnswer = true))
        if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
            _state.update {
                it.copy(question = result.getOrThrow().random())
            }
        } else {
            _event.emit(AnswerSecurityQuestionEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private fun verifySecurityQuestion() {
        viewModelScope.launch {
            val state = _state.value
            if (state.question == null || state.answer.isBlank()) {
                return@launch
            }
            val result = verifySecurityQuestionUseCase(
                listOf(
                    QuestionsAndAnswer(state.question.id, state.answer)
                )
            )
            if (result.isSuccess) {
                _event.emit(AnswerSecurityQuestionEvent.OnVerifySuccess(result.getOrThrow()))
            } else {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message.orUnknownError())
                }
            }
        }
    }

    private fun downloadBackupKey(signer: SignerModel) = viewModelScope.launch {
        val state = _state.value
        if (state.question == null || state.answer.isBlank()) {
            return@launch
        }
        _event.emit(AnswerSecurityQuestionEvent.Loading(true))
        val result = downloadBackupKeyUseCase(
            DownloadBackupKeyUseCase.Param(
                id = signer.fingerPrint,
                questionId = state.question.id,
                answer = state.answer,
                verifyToken = args.verifyToken
            )
        )
        _event.emit(AnswerSecurityQuestionEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(
                AnswerSecurityQuestionEvent.DownloadBackupKeySuccess(
                    signer,
                    result.getOrThrow()
                )
            )
        } else {
            val exception = result.exceptionOrNull()
            if (exception is NunchukApiException && exception.code == 400) {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message.orUnknownError())
                }
            } else {
                _event.emit(AnswerSecurityQuestionEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onAnswerTextChange(answer: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(answer = answer)
            }
        }
    }
}