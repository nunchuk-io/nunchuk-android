package com.nunchuk.android.transaction.components.receive.address.unused

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.transaction.components.receive.address.unused.UnusedAddressEvent.GenerateAddressErrorEvent
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UnusedAddressViewModel @Inject constructor(
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase
) : NunchukViewModel<UnusedAddressState, UnusedAddressEvent>() {

    private lateinit var walletId: String

    override val initialState = UnusedAddressState()

    fun init(walletId: String) {
        this.walletId = walletId
        getUnusedAddresses()
    }

    private fun getUnusedAddresses() {
        viewModelScope.launch {
            addressesUseCase.execute(walletId = walletId)
                .onException { onError() }
                .collect { onSuccess(it) }
        }

    }

    private fun onError() {
        updateState { copy(addresses = emptyList()) }
    }

    private fun onSuccess(addresses: List<String>) {
        updateState { copy(addresses = addresses) }
        if (addresses.isEmpty()) {
            generateAddress()
        }
    }

    fun generateAddress() {
        viewModelScope.launch {
            newAddressUseCase.execute(walletId = walletId)
                .onException { event(GenerateAddressErrorEvent(it.message.orEmpty())) }
                .collect { getUnusedAddresses() }
        }
    }

}