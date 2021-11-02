package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ExportKeystoneTransactionUseCase {
    fun execute(walletId: String, txId: String): Flow<List<String>>
}

internal class ExportKeystoneTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ExportKeystoneTransactionUseCase {

    override fun execute(walletId: String, txId: String) = flow {
        emit(nativeSdk.exportKeystoneTransaction(walletId = walletId, txId = txId))
    }

}
