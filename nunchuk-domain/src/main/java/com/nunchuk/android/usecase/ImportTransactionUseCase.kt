package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface ImportTransactionUseCase {
    fun execute(walletId: String, filePath: String, initEventId: String = "", masterFingerPrint: String = ""): Flow<Transaction>
}

internal class ImportTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportTransactionUseCase {

    override fun execute(walletId: String, filePath: String, initEventId: String, masterFingerPrint: String): Flow<Transaction> = flow {
        val result = nativeSdk.importTransaction(walletId = walletId, filePath = filePath)
        if (masterFingerPrint.isNotEmpty() && initEventId.isNotEmpty()) {
            nativeSdk.signAirgapTransaction(initEventId, masterFingerPrint)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

}
