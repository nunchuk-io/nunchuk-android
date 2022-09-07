package com.nunchuk.android.contact.components.pending.sent

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.components.pending.sent.SentEvent.LoadingEvent
import com.nunchuk.android.contact.usecase.CancelContactsUseCase
import com.nunchuk.android.contact.usecase.GetSentContactsUseCase
import com.nunchuk.android.model.SentContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SentViewModel @Inject constructor(
    private val getSentContactsUseCase: GetSentContactsUseCase,
    private val cancelContactsUseCase: CancelContactsUseCase,
) : NunchukViewModel<SentState, SentEvent>() {

    override val initialState: SentState = SentState()

    fun retrieveData() {
        viewModelScope.launch {
            val result = getSentContactsUseCase(Unit)
            if (result.isSuccess) {
                updateState { copy(contacts = result.getOrThrow()) }
            } else {
                updateState { copy(contacts = emptyList()) }
            }
        }
    }

    fun handleWithDraw(contact: SentContact) {
        event(LoadingEvent(true))
        viewModelScope.launch {
            event(LoadingEvent(true))
            val result = cancelContactsUseCase(contact.contact.id)
            event(LoadingEvent(false))
            if (result.isSuccess) {
                retrieveData()
            }

        }
    }
}