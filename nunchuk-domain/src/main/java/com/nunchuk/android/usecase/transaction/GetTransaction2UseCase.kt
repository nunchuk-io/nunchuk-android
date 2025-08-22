package com.nunchuk.android.usecase.transaction

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTransaction2UseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<GetTransaction2UseCase.Params, Transaction>(ioDispatcher) {
    override suspend fun execute(parameters: Params): Transaction {
        return nativeSdk.getTransaction(txId = parameters.txId, walletId = parameters.walletId)
    }

    class Params(
        val walletId: String,
        val txId: String
    )
}