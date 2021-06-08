package com.nunchuk.android.transaction.export

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.export.ExportTransactionEvent.ExportToFileSuccess
import com.nunchuk.android.transaction.export.ExportTransactionEvent.ExportTransactionError
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportCoboTransactionUseCase
import com.nunchuk.android.usecase.ExportTransactionUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ExportTransactionViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportTransactionUseCase: ExportTransactionUseCase,
    private val exportCoboTransactionUseCase: ExportCoboTransactionUseCase
) : NunchukViewModel<ExportTransactionState, ExportTransactionEvent>() {

    private lateinit var walletId: String
    private lateinit var txId: String

    override val initialState = ExportTransactionState()

    fun init(walletId: String, txId: String) {
        this.walletId = walletId
        this.txId = txId
        exportTransactionToQRs()
    }

    fun exportTransactionToFile() {
        viewModelScope.launch {
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
        viewModelScope.launch {
            when (val result = exportCoboTransactionUseCase.execute(walletId, txId)) {
                is Success -> updateState { copy(qrcode = result.data) }
                is Error -> event(ExportTransactionError(result.exception.messageOrUnknownError()))
            }
        }
    }


}