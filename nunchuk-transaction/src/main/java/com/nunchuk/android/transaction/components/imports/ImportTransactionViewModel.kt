package com.nunchuk.android.transaction.components.imports

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.usecase.ImportTransactionUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ImportTransactionViewModel @Inject constructor(
    private val importTransactionUseCase: ImportTransactionUseCase
) : NunchukViewModel<Unit, ImportTransactionEvent>() {

    private lateinit var walletId: String

    override val initialState = Unit

    fun init(walletId: String) {
        this.walletId = walletId
    }

    fun importTransaction(filePath: String) {
        viewModelScope.launch {
            when (val result = importTransactionUseCase.execute(walletId, filePath)) {
                is Result.Error -> ImportTransactionError(result.exception.messageOrUnknownError())
                is Result.Success -> event(ImportTransactionSuccess)
            }
        }
    }


}