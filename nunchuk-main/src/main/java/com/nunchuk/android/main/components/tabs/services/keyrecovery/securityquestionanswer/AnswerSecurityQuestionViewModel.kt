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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.securityquestionanswer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetSecurityQuestionUseCase
import com.nunchuk.android.core.domain.membership.VerifySecurityQuestionUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.QuestionsAndAnswer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnswerSecurityQuestionViewModel @Inject constructor(
    private val getSecurityQuestionUseCase: GetSecurityQuestionUseCase,
    private val verifySecurityQuestionUseCase: VerifySecurityQuestionUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<AnswerSecurityQuestionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(AnswerSecurityQuestionState())
    val state = _state.asStateFlow()

    init {
        getSecurityQuestion()
    }

    fun onContinueClicked() {
        verifySecurityQuestion()
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
                _event.emit(
                    AnswerSecurityQuestionEvent.OnVerifySuccess(
                        result.getOrThrow(),
                        state.answer,
                        state.question.id
                    )
                )
            } else {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message.orUnknownError())
                }
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