package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ExportPassportTransactionUseCase {
    fun execute(walletId: String, txId: String): Flow<List<String>>
}

internal class ExportPassportTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ExportPassportTransactionUseCase {

    override fun execute(walletId: String, txId: String) = flow {
        emit(nativeSdk.exportPassportTransaction(walletId = walletId, txId = txId))
    }

}