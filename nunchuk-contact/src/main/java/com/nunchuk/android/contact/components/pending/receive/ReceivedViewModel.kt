package com.nunchuk.android.contact.components.pending.receive

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.contact.usecase.AcceptContactsUseCase
import com.nunchuk.android.contact.usecase.CancelContactsUseCase
import com.nunchuk.android.contact.usecase.GetReceivedContactsUseCase
import com.nunchuk.android.model.ReceiveContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceivedViewModel @Inject constructor(
    private val getReceivedContactsUseCase: GetReceivedContactsUseCase,
    private val acceptContactsUseCase: AcceptContactsUseCase,
    private val cancelContactsUseCase: CancelContactsUseCase
) : NunchukViewModel<ReceivedState, ReceivedEvent>() {

    override val initialState: ReceivedState = ReceivedState()

    fun retrieveData() {
        viewModelScope.launch {
            val result = getReceivedContactsUseCase(Unit)
            if (result.isSuccess) {
                updateState { copy(contacts = result.getOrThrow()) }
            } else {
                updateState { copy(contacts = emptyList()) }
            }
        }
    }

    fun handleAcceptRequest(contact: ReceiveContact) {
        viewModelScope.launch {
            event(ReceivedEvent.LoadingEvent(true))
            val result = acceptContactsUseCase(contact.contact.id)
            event(ReceivedEvent.LoadingEvent(false))
            if (result.isSuccess) {
                retrieveData()
            }
        }
    }

    fun handleCancelRequest(contact: ReceiveContact) {
        viewModelScope.launch {
            event(ReceivedEvent.LoadingEvent(true))
            val result = cancelContactsUseCase(contact.contact.id)
            event(ReceivedEvent.LoadingEvent(false))
            if (result.isSuccess) {
                retrieveData()
            }
        }
    }
}