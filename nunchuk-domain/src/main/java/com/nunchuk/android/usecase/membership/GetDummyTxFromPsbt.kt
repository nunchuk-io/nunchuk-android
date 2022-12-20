package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetDummyTxFromPsbt @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
     @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<GetDummyTxFromPsbt.Param, Transaction>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Transaction {
        return nativeSdk.getDummyTx(parameters.walletId, parameters.psbt)
    }

    data class Param(val walletId: String, val psbt: String)
}