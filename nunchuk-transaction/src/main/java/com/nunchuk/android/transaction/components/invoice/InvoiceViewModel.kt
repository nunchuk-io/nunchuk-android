package com.nunchuk.android.transaction.components.invoice

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.membership.SaveBitmapToPDFUseCase
import com.nunchuk.android.utils.BitmapUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val saveBitmapToPDFUseCase: SaveBitmapToPDFUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<InvoiceEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InvoiceState())
    val state = _state.asStateFlow()

    fun saveBitmapToPDF(bitmaps: List<Bitmap>, fileName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val combineBitmap = BitmapUtil.combineBitmapsVertically(bitmaps)
            when (val event = createShareFileUseCase.execute("$fileName.pdf")) {
                is Result.Success -> {
                    saveBitmapToPDFUseCase(
                        SaveBitmapToPDFUseCase.Param(
                            listOf(combineBitmap),
                            event.data
                        )
                    )
                        .onSuccess {
                            _event.emit(InvoiceEvent.ShareFile(event.data))
                        }
                }

                is Result.Error -> {
                    _event.emit(InvoiceEvent.Error(event.exception.messageOrUnknownError()))
                }
            }
        }
}

data class InvoiceState(
    val bitmap: Bitmap? = null,
)

sealed class InvoiceEvent {
    data class Error(val message: String) : InvoiceEvent()
    data class Loading(val loading: Boolean) : InvoiceEvent()
    data class ShareFile(val filePath: String) : InvoiceEvent()
}