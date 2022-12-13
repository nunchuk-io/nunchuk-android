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
            }
            _event.emit(NonSubscriberIntroEvent.Loading(true))
            val result = submitEmailUseCase(SubmitEmailUseCase.Param(
                bannerId = email,
                email = args.bannerId,
            ))
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