package com.nunchuk.android.transaction.components.send.receipt

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.*
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AddReceiptViewModel @Inject constructor(
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val parseBtcUriUseCase: ParseBtcUriUseCase
) : NunchukViewModel<AddReceiptState, AddReceiptEvent>() {

    override val initialState = AddReceiptState()

    fun init(address: String, privateNote: String) {
        updateState { initialState.copy(address = address, privateNote = privateNote) }
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                updateState { copy(address = btcUri.address, privateNote = btcUri.privateNote, amount = btcUri.amount) }
            } else {
                setEvent(ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleContinueEvent(isCreateTransaction: Boolean) {
        viewModelScope.launch {
            val currentState = getState()
            val address = currentState.address
            when {
                address.isEmpty() -> event(AddressRequiredEvent)
                else -> when (checkAddressValidUseCase.execute(address = address)) {
                    is Success -> setEvent(AcceptedAddressEvent(address, currentState.privateNote, currentState.amount, isCreateTransaction))
                    is Error -> setEvent(InvalidAddressEvent)
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

    fun getAddReceiptState() = getState()
}