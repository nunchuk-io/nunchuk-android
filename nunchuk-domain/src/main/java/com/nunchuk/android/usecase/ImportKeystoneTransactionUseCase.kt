package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ImportKeystoneTransactionUseCase {
    fun execute(walletId: String, qrData: List<String>): Flow<Transaction>
}

internal class ImportKeystoneTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportKeystoneTransactionUseCase {

    override fun execute(walletId: String, qrData: List<String>) = flow {
        emit(nativeSdk.importKeystoneTransaction(walletId = walletId, qrData = qrData))
    }

}
