package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateTransactionMemo @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<UpdateTransactionMemo.Data, Boolean>(dispatcher) {
    override suspend fun execute(parameters: Data) : Boolean {
        return nativeSdk.updateTransactionMemo(parameters.walletId, parameters.txId, parameters.newMemo)
    }

    class Data(val walletId: String, val txId: String, val newMemo: String)
}