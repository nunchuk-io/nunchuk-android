package com.nunchuk.android.signer.components.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.JoinFreeGroupWalletUseCase
import com.nunchuk.android.model.GroupSandbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanDynamicQRViewModel @Inject constructor(
    private val joinFreeGroupWalletUseCase: JoinFreeGroupWalletUseCase
) : ViewModel() {
    private var isHandlingQRContent = false

    private val _event = MutableSharedFlow<ScanDynamicQREvent>()
    val event = _event.asSharedFlow()

    fun handleJoinGroupWallet(content: String) = viewModelScope.launch {
        if (isHandlingQRContent) return@launch
        isHandlingQRContent = true
        joinFreeGroupWalletUseCase(content).onSuccess {
            _event.emit(ScanDynamicQREvent.JoinGroupWalletSuccess(it))
        }.onFailure {
            isHandlingQRContent = false
        }
    }
}

sealed class ScanDynamicQREvent {
    data class JoinGroupWalletSuccess(val groupSandbox: GroupSandbox) : ScanDynamicQREvent()
    data class Error(val message: String) : ScanDynamicQREvent()
}