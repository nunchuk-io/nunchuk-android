package com.nunchuk.android.transaction.components.send.receipt

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.*
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AddReceiptViewModel @Inject constructor(
    private val checkAddressValidUseCase: CheckAddressValidUseCase
) : NunchukViewModel<AddReceiptState, AddReceiptEvent>() {

    override val initialState = AddReceiptState()

    fun init() {
        updateState { initialState }
    }

    fun handleContinueEvent() {
        viewModelScope.launch {
            val currentState = getState()
            val address = currentState.address
            when {
                address.isEmpty() -> event(AddressRequiredEvent)
                else -> when (checkAddressValidUseCase.execute(address = address)) {
                    is Success -> event(AcceptedAddressEvent(address, currentState.privateNote))
                    is Error -> event(InvalidAddressEvent)
                }
            }
        }
    }

    fun handleReceiptChanged(address: String) {
        updateState { copy(address = address) }
    }

    fun handlePrivateNoteChanged(privateNote: String) {
        updateState { copy(privateNote = privateNote) }
    }

}