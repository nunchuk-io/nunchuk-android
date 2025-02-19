package com.nunchuk.android.main.groupwallet.join

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.qr.shareBitmap
import com.nunchuk.android.domain.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommonQRCodeViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _event = MutableSharedFlow<CommonQRCodeEvent>()
    val event = _event.asSharedFlow()

    private val _uiState = MutableStateFlow<CommonQRCodeScreenState>(CommonQRCodeScreenState())
    val uiState = _uiState.asStateFlow()

    fun generateQRCode(url: String) {
        viewModelScope.launch(ioDispatcher) {
            val bitmap = url.convertToQRCode()
            _uiState.update {
                it.copy(qrCodeBitmap = bitmap)
            }
        }
    }

    fun shareQRCode(bitmap: Bitmap) {
        viewModelScope.launch(ioDispatcher) {
            val filePath = shareBitmap(context, bitmap)
            if (filePath != null) {
                _event.emit(CommonQRCodeEvent.ShareQRCodeSuccess(filePath))
            } else {
                _event.emit(CommonQRCodeEvent.Error("Failed to share QR code"))
            }
        }
    }
}

data class CommonQRCodeScreenState(val qrCodeBitmap: Bitmap? = null)

sealed class CommonQRCodeEvent {
    data class ShareQRCodeSuccess(val filePath: String) : CommonQRCodeEvent()
    data class Error(val message: String) : CommonQRCodeEvent()
}