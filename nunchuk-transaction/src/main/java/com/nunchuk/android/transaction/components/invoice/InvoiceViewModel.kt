package com.nunchuk.android.transaction.components.invoice

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.wallet.InvoiceInfo
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.membership.SaveBitmapToPDFUseCase
import com.nunchuk.android.utils.ExportInvoices
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _event = MutableSharedFlow<InvoiceEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InvoiceState())
    val state = _state.asStateFlow()

    fun exportInvoice(invoiceInfo: InvoiceInfo, txId: String, isSaveFile: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = "Transaction_$txId.pdf"
            when (val event = createShareFileUseCase.execute(fileName)) {
                is Result.Success -> {
                    ExportInvoices(context = context).generatePDF(listOf(invoiceInfo), event.data, Job())
                    if (isSaveFile) {
                        saveLocalFile(event.data, fileName)
                    } else {
                        _event.emit(InvoiceEvent.ShareFile(event.data))
                    }
                }

                is Result.Error -> {
                    _event.emit(InvoiceEvent.Error(event.exception.messageOrUnknownError()))
                }
            }
        }
    }

    fun saveLocalFile(filePath: String, fileName: String) {
        viewModelScope.launch {
            val result = saveLocalFileUseCase(SaveLocalFileUseCase.Params(fileName = fileName, filePath = filePath))
            _event.emit(InvoiceEvent.SaveLocalFile(result.isSuccess))
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
    data class SaveLocalFile(val isSuccess: Boolean) : InvoiceEvent()
}