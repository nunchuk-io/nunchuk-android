package com.nunchuk.android.transaction.components.receive.address.used

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.components.receive.address.UsedAddressModel
import com.nunchuk.android.transaction.components.receive.address.used.UsedAddressEvent.GetUsedAddressErrorEvent
import com.nunchuk.android.usecase.GetAddressBalanceUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class UsedAddressViewModel @Inject constructor(
    private val getAddressesUseCase: GetAddressesUseCase,
    private val getAddressBalanceUseCase: GetAddressBalanceUseCase
) : NunchukViewModel<UsedAddressState, UsedAddressEvent>() {

    private lateinit var walletId: String

    override val initialState = UsedAddressState()

    fun init(walletId: String) {
        this.walletId = walletId
        getUnusedAddress()
    }

    private fun getUnusedAddress() {
        viewModelScope.launch {
            getAddressesUseCase.execute(walletId = walletId, used = true)
                .catch { event(GetUsedAddressErrorEvent(it.message.orEmpty())) }
                .collect { getAddressBalance(it) }
        }
    }

    private fun getAddressBalance(addresses: List<String>) {
        viewModelScope.launch {
            val addressModels = addresses.map {
                when (val result = getAddressBalanceUseCase.execute(walletId, it)) {
                    is Success -> UsedAddressModel(it, result.data)
                    is Error -> UsedAddressModel(it, Amount.ZER0)
                }
            }
            updateState { copy(addresses = addressModels) }
        }
    }

}