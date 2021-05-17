package com.nunchuk.android.transaction.receive.address.unused

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            when (val result = addressesUseCase.execute(walletId = walletId, used = false, internal = false)) {
                is Error -> updateState { copy(addresses = emptyList()) }
                is Success -> updateState { copy(addresses = result.data) }
            }
        }

    }

}