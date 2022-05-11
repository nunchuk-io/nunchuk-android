package com.nunchuk.android.transaction.components.imports

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.usecase.ImportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.ImportPassportTransactionUseCase
import com.nunchuk.android.usecase.ImportTransactionUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class ImportTransactionViewModel @Inject constructor(
    private val importTransactionUseCase: ImportTransactionUseCase,
    private val importKeystoneTransactionUseCase: ImportKeystoneTransactionUseCase,
    private val importPassportTransactionUseCase: ImportPassportTransactionUseCase
) : NunchukViewModel<Unit, ImportTransactionEvent>() {

    private lateinit var walletId: String
    private lateinit var transactionOption: TransactionOption
    private var isProcessing = false

    private val qrDataList = HashSet<String>()

    override val initialState = Unit

    fun init(walletId: String, transactionOption: TransactionOption) {
        this.walletId = walletId
        this.transactionOption = transactionOption
    }

    fun importTransaction(filePath: String) {
        viewModelScope.launch {
            importTransactionUseCase.execute(walletId, filePath)
                .flowOn(IO)
                .onException { event(ImportTransactionError(it.readableMessage())) }
                .flowOn(Main)
                .collect { event(ImportTransactionSuccess) }
        }
    }

    fun updateQRCode(qrData: String) {
        qrDataList.add(qrData)
        Timber.d("[ImportTransaction]updateQRCode($qrData)")
        Timber.d("[ImportTransaction]isProcessing::$isProcessing")
        if (!isProcessing) {
            viewModelScope.launch {
                Timber.d("[ImportTransaction]execute($walletId, $qrDataList)")
                if (transactionOption == TransactionOption.IMPORT_PASSPORT) {
                    importPassportTransactionUseCase.execute(walletId = walletId, qrData = qrDataList.toList())
                } else {
                    importKeystoneTransactionUseCase.execute(walletId = walletId, qrData = qrDataList.toList())
                }
                    .onStart { isProcessing = true }
                    .flowOn(IO)
                    .onException { event(ImportTransactionError(it.readableMessage())) }
                    .flowOn(Main)
                    .onCompletion { isProcessing = false }
                    .collect { event(ImportTransactionSuccess) }
            }
        }
    }

}