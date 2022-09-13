package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ImportKeystoneTransactionUseCase {
    fun execute(walletId: String, qrData: List<String>, initEventId: String = "", masterFingerPrint: String = ""): Flow<Transaction>
}

internal class ImportKeystoneTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportKeystoneTransactionUseCase {

    override fun execute(walletId: String, qrData: List<String>, initEventId: String, masterFingerPrint: String): Flow<Transaction> = flow {
        val result = nativeSdk.importKeystoneTransaction(walletId = walletId, qrData = qrData)
        if (masterFingerPrint.isNotEmpty() && initEventId.isNotEmpty()) {
            nativeSdk.signAirgapTransaction(initEventId, masterFingerPrint)
        }
        emit(result)
    }

}
