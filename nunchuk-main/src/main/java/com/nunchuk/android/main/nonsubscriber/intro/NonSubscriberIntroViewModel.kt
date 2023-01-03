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

package com.nunchuk.android.main.nonsubscriber.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.main.nonsubscriber.intro.model.AssistedWalletPoint
import com.nunchuk.android.usecase.banner.GetAssistedWalletPageContentUseCase
import com.nunchuk.android.usecase.banner.SubmitEmailUseCase
import com.nunchuk.android.utils.EmailValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NonSubscriberIntroViewModel @Inject constructor(
    private val getAssistedWalletPageContentUseCase: GetAssistedWalletPageContentUseCase,
    private val submitEmailUseCase: SubmitEmailUseCase,
    private val accountManager: AccountManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = NonSubscriberIntroFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<NonSubscriberIntroEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(NonSubscriberState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = getAssistedWalletPageContentUseCase(args.bannerId)
            if (result.isSuccess) {
                val content = result.getOrThrow()
                _state.update {
                    it.copy(
                        title = content.title,
                        desc = content.desc,
                        items = content.items.map { item ->
                            AssistedWalletPoint(
                                title = item.title,
                                desc = item.desc,
                                iconUrl = item.url
                            )
                        }
                    )
                }
            } else {
                _event.emit(NonSubscriberIntroEvent.ShowError(result.exceptionOrNull()?.message.orEmpty()))
            }
        }
    }

    fun submitEmail(email: String) {
        viewModelScope.launch {
            if (EmailValidator.valid(email).not()) {
                _event.emit(NonSubscriberIntroEvent.EmailInvalid)
                return@launch
            }
            _event.emit(NonSubscriberIntroEvent.Loading(true))
            val result = submitEmailUseCase(SubmitEmailUseCase.Param(
                email = email,
                bannerId = args.bannerId,
            ))
            _event.emit(NonSubscriberIntroEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(NonSubscriberIntroEvent.OnSubmitEmailSuccess(email))
            } else {
                _event.emit(NonSubscriberIntroEvent.ShowError(result.exceptionOrNull()?.message.orEmpty()))
            }
        }
    }

    fun getEmail() = accountManager.getAccount().email
}

sealed class NonSubscriberIntroEvent {
    object EmailInvalid : NonSubscriberIntroEvent()
    data class Loading(val isLoading: Boolean) : NonSubscriberIntroEvent()
    data class ShowError(val message: String) : NonSubscriberIntroEvent()
    data class OnSubmitEmailSuccess(val email: String) : NonSubscriberIntroEvent()
}