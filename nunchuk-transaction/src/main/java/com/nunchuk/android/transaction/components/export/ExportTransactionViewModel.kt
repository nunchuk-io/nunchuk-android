package com.nunchuk.android.transaction.components.export

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.transaction.components.export.ExportTransactionEvent.*
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.ExportPassportTransactionUseCase
import com.nunchuk.android.usecase.ExportTransactionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ExportTransactionViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportTransactionUseCase: ExportTransactionUseCase,
    private val exportPassportTransactionUseCase: ExportPassportTransactionUseCase,
    private val exportKeystoneTransactionUseCase: ExportKeystoneTransactionUseCase,
) : NunchukViewModel<ExportTransactionState, ExportTransactionEvent>() {

    private lateinit var walletId: String
    private lateinit var txId: String
    private lateinit var transactionOption: TransactionOption

    override val initialState = ExportTransactionState()

    fun init(walletId: String, txId: String, transactionOption: TransactionOption) {
        this.walletId = walletId
        this.txId = txId
        this.transactionOption = transactionOption
        handleExportTransactionQRs()
    }

    private fun handleExportTransactionQRs() {
        if (transactionOption == TransactionOption.EXPORT_PASSPORT) {
            exportPassportTransaction()
        } else {
            exportTransactionToQRs()
        }
    }

    fun exportTransactionToFile() {
        viewModelScope.launch {
            event(LoadingEvent)
            when (val result = createShareFileUseCase.execute("${walletId}_${txId}")) {
                is Success -> exportTransaction(result.data)
                is Error -> event(ExportTransactionError(result.exception.messageOrUnknownError()))
            }
        }
    }

    private fun exportTransaction(filePath: String) {
        viewModelScope.launch {
            when (val result = exportTransactionUseCase.execute(walletId, txId, filePath)) {
                is Success -> event(ExportToFileSuccess(filePath))
                is Error -> event(ExportTransactionError(result.exception.messageOrUnknownError()))
            }
        }
    }

    private fun exportTransactionToQRs() {
        val qrSize = getQrSize()
        viewModelScope.launch {
            exportKeystoneTransactionUseCase.execute(walletId, txId)
                .map { it.mapNotNull { qrCode -> qrCode.convertToQRCode(qrSize, qrSize) } }
                .flowOn(IO)
                .onException { event(ExportTransactionError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect { updateState { copy(qrCodeBitmap = it) } }
        }
    }

    private fun exportPassportTransaction() {
        val qrSize = getQrSize()
        viewModelScope.launch {
            exportPassportTransactionUseCase.execute(walletId, txId)
                .map { it.mapNotNull { qrCode -> qrCode.convertToQRCode(qrSize, qrSize) } }
                .flowOn(IO)
                .onException { event(ExportTransactionError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect { updateState { copy(qrCodeBitmap = it) } }
        }
    }

    fun getQrSize(): Int {
        return Resources.getSystem().displayMetrics.widthPixels * 3 / 4
    }
}