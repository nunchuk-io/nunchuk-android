package com.nunchuk.android.transaction.export

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.usecase.ExportCoboTransactionUseCase
import com.nunchuk.android.usecase.ExportTransactionUseCase
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
    }

}