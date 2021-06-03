package com.nunchuk.android.transaction.export

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.export.ExportTransactionEvent.ExportTransactionError
import com.nunchuk.android.usecase.ExportCoboTransactionUseCase
import com.nunchuk.android.usecase.ExportTransactionUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ExportTransactionViewModel @Inject constructor(
    private val exportTransactionUseCase: ExportTransactionUseCase,
    private val exportCoboTransactionUseCase: ExportCoboTransactionUseCase
) : NunchukViewModel<ExportTransactionState, ExportTransactionEvent>() {

    private lateinit var walletId: String
    private lateinit var txId: String

    override val initialState = ExportTransactionState()

    fun init(walletId: String, txId: String) {
        this.walletId = walletId
        this.txId = txId

        exportTransaction()
        exportCoboTransaction()
    }

    private fun exportTransaction() {
        // TODO
    }

    private fun exportCoboTransaction() {
        viewModelScope.launch {
            when (val result = exportCoboTransactionUseCase.execute(walletId, txId)) {
                is Success -> updateState { copy(qrcode = result.data) }
                is Error -> event(ExportTransactionError(result.exception.messageOrUnknownError()))
            }
        }
    }


}