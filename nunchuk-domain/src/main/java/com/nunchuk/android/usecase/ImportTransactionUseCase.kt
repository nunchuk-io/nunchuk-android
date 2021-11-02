package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ImportTransactionUseCase {
    fun execute(walletId: String, filePath: String): Flow<Transaction>
}

internal class ImportTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportTransactionUseCase {

    override fun execute(walletId: String, filePath: String) = flow {
        emit(
            nativeSdk.importTransaction(
                walletId = walletId,
                filePath = filePath
            )
        )
    }
}
