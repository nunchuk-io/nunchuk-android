package com.nunchuk.android.main.components.tabs.services.keyrecovery.securityquestionanswer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.DownloadBackupKeyUseCase
import com.nunchuk.android.core.domain.membership.GetSecurityQuestionUseCase
import com.nunchuk.android.core.util.orUnknownError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyRecoveryAnswerSecurityQuestionViewModel @Inject constructor(
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val downloadBackupKeyUseCase: DownloadBackupKeyUseCase,
    savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    private val args = AnswerSecurityQuestionFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<AnswerSecurityQuestionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(AnswerSecurityQuestionState())
    val state = _state.asStateFlow()

    init {
        getSecurityQuestion()
    }

    private fun getSecurityQuestion() = viewModelScope.launch {
        val result =
            getSecurityQuestionUseCase(GetSecurityQuestionUseCase.Param(isFilterAnswer = true))
        if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
            _state.update {
                it.copy(question = result.getOrThrow().random())
            }
        }
    }

    fun downloadBackupKey() = viewModelScope.launch {
        val state = _state.value
        if (state.question == null || state.answer.isBlank()) {
            return@launch
        }
        _event.emit(AnswerSecurityQuestionEvent.Loading(true))
        val result = downloadBackupKeyUseCase(
            DownloadBackupKeyUseCase.Param(
                id = args.signer.fingerPrint,
                questionId = state.question.id,
                answer = state.answer,
                verifyToken = args.verifyToken
            )
        )
        _event.emit(AnswerSecurityQuestionEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(AnswerSecurityQuestionEvent.DownloadBackupKeySuccess(result.getOrThrow()))
        } else {
            _event.emit(AnswerSecurityQuestionEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
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