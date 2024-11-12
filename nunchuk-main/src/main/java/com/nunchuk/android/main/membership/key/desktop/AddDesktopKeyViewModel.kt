package com.nunchuk.android.main.membership.key.desktop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.usecase.membership.RequestAddDesktopKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDesktopKeyViewModel @Inject constructor(
    private val requestAddDesktopKeyUseCase: RequestAddDesktopKeyUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = AddDesktopKeyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<AddDesktopKeyEvent>()
    val event = _event.asSharedFlow()

    private var requestJob: Job? = null

    fun requestAddDesktopKey() {
        if (requestJob?.isActive != true) {
            requestJob = viewModelScope.launch {
                requestAddDesktopKeyUseCase(
                    RequestAddDesktopKeyUseCase.Param(
                        args.step,
                        args.groupId.orEmpty(),
                        if (args.isAddInheritanceKey) listOf(SignerTag.INHERITANCE, args.signerTag) else listOf(args.signerTag)
                    )
                ).onSuccess {
                    _event.emit(AddDesktopKeyEvent.RequestAddKeySuccess(it))
                }.onFailure {
                    _event.emit(AddDesktopKeyEvent.RequestAddKeyFailed(it.message.orUnknownError()))
                }
            }
        }
    }
}

sealed class AddDesktopKeyEvent {
    data class RequestAddKeySuccess(val requestId: String,) : AddDesktopKeyEvent()
    data class RequestAddKeyFailed(val message: String) : AddDesktopKeyEvent()
}