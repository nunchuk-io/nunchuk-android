package com.nunchuk.android.auth.components.enterxpub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.usecase.GetSignInDummyTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnterXPUBViewModel @Inject constructor(
    val getSigninDummyTransactionUseCase: GetSignInDummyTransactionUseCase
) : ViewModel() {

    private val _event = MutableSharedFlow<EnterXPUBEvent>()
    val event = _event.asSharedFlow()

    fun signinDummy(data: String) {
//        val data = "BSMS 1.0\n" +
//                "wsh(sortedmulti(2,[85867215/48'/1'/0'/2']tpubDDuXvjq5jan2DE3bAEcmWGMw5DC2rEw5qW18tMfHgjyiXbokFPkCrFcfWcLW9Rsn9o3W5nN7o3cF6TeUA3WGHm4gUJtj958ujYhoeM4Yg6T/**,[0c87f240/48'/1'/0'/2']tpubDDuXvjq5jan2Dqu49mEmRxJKYdcw6Phgb2QGoNAJUCdDcbNjYoamxj8757Ktkzu5dvwgZVGHatnvL96vkiATs9kBxPAPeg84Xn3m4nrGwJU/**,[32819d34/48'/0'/3'/2']tpubDFdxoDkSanCmfFCSPyNJgLwUsENbZ8B8Z9ZM3xqNQGF2v691JnwnZUuKHXUqdWVVthXRALpfh6du7cChS58b3LjkBxKAL3pKX6cFeKk4f11/**,[cfd3794f/48'/1'/208'/2']tpubDFWMTptHMG1XWR7QWCK2MVgsBTykCwJHNJx7v8b3imcwHbevu7THVCnp9qZkvEwg8wS1VHLe5C1SMBSra7TDKeBUmtcf2b1rotkfs1CgEHk/**))\n" +
//                "/0/*,/1/*\n" +
//                "tb1qzkga4c9rnrgjkch8yk3243qwcxk4mkgxr56vc9kgvda699vwcm8se4vrt7"
        viewModelScope.launch {
            _event.emit(EnterXPUBEvent.Loading(true))
            getSigninDummyTransactionUseCase(
                GetSignInDummyTransactionUseCase.Param(
                    data = data
                )
            ).onSuccess {
                _event.emit(EnterXPUBEvent.Success(
                    requiredSignatures = it.requiredSignatures,
                    dummyTransactionId = it.dummyTransactionId,
                    signInData = data
                ))
            }.onFailure {
                _event.emit(EnterXPUBEvent.Error(it.message.orUnknownError()))
            }
            _event.emit(EnterXPUBEvent.Loading(false))
        }
    }
}

sealed class EnterXPUBEvent {
    data class Loading(val loading: Boolean) : EnterXPUBEvent()
    data class Success(
        val requiredSignatures: Int,
        val dummyTransactionId: String,
        val signInData: String
    ) : EnterXPUBEvent()
    data class Error(val message: String) : EnterXPUBEvent()
}