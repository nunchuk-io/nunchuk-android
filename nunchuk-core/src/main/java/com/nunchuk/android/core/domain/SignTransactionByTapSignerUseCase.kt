package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.data.WaitTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignTransactionByTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitTapSignerUseCase: WaitTapSignerUseCase
) : BaseNfcUseCase<SignTransactionByTapSignerUseCase.Data, Transaction>(dispatcher, waitTapSignerUseCase) {

    override suspend fun executeNfc(parameters: Data): Transaction {
        return nunchukNativeSdk.signTransactionByTapSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            walletId = parameters.walletId,
            txId = parameters.txId
        )
    }

    class Data(isoDep: IsoDep, val cvc: String, val walletId: String, val txId: String) : BaseNfcUseCase.Data(isoDep)
}