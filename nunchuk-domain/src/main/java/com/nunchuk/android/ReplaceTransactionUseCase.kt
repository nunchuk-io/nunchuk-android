package com.nunchuk.android

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ReplaceTransactionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<ReplaceTransactionUseCase.Data, Transaction>(dispatcher) {

    override suspend fun execute(parameters: Data): Transaction {
        return nativeSdk.replaceTransaction(parameters.walletId, parameters.txId, Amount(value = parameters.newFee.toLong()))
    }

    data class Data(val walletId: String, val txId: String, val newFee: Int)
}