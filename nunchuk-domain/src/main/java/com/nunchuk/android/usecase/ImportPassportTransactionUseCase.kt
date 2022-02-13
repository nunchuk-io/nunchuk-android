package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ImportPassportTransactionUseCase {
    fun execute(walletId: String, qrData: List<String>): Flow<Transaction>
}

internal class ImportPassportTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportPassportTransactionUseCase {

    override fun execute(walletId: String, qrData: List<String>) = flow {
        emit(nativeSdk.importPassportTransaction(walletId = walletId, qrData = qrData))
    }

}