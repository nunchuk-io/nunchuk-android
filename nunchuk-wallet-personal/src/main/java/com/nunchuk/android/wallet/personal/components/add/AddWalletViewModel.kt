package com.nunchuk.android.wallet.personal.components.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.GetFreeGroupWalletConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddWalletViewModel @Inject constructor(
    private val getFreeGroupWalletConfigUseCase: GetFreeGroupWalletConfigUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddWalletState())
    val state = _state.asStateFlow()

    init {
        getFreeGroupWalletConfig(AddressType.NATIVE_SEGWIT)
    }

    fun getFreeGroupWalletConfig(addressType: AddressType) = viewModelScope.launch {
        viewModelScope.launch {
            getFreeGroupWalletConfigUseCase(addressType)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            freeGroupWalletConfig = result
                        )
                    }
                }.onFailure {
                    Log.e("group-wallet", "Failed to get free group wallet config $it")
                }
        }
    }
}