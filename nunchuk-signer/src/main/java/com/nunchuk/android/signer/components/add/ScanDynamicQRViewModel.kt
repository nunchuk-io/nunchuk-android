package com.nunchuk.android.signer.components.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.JoinFreeGroupWalletUseCase
import com.nunchuk.android.core.domain.ParseQRCodeFromPhotoUseCase
import com.nunchuk.android.model.GroupSandbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanDynamicQRViewModel @Inject constructor(
    private val joinFreeGroupWalletUseCase: JoinFreeGroupWalletUseCase,
    private val parseQRCodeFromPhotoUseCase: ParseQRCodeFromPhotoUseCase
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
            _event.emit(ScanDynamicQREvent.Error(it.message ?: "Unknown error"))
        }
    }

    fun decodeQRCodeFromUri(uri: Uri) {
        viewModelScope.launch {
            parseQRCodeFromPhotoUseCase(uri).onSuccess {
                _event.emit(ScanDynamicQREvent.ParseQRCodeSuccess(it))
            }.onFailure {
                _event.emit(ScanDynamicQREvent.Error(it.message ?: "Unknown error"))
            }
        }
    }
}

sealed class ScanDynamicQREvent {
    data class JoinGroupWalletSuccess(val groupSandbox: GroupSandbox) : ScanDynamicQREvent()
    data class Error(val message: String) : ScanDynamicQREvent()
    data class ParseQRCodeSuccess(val content: String) : ScanDynamicQREvent()
}