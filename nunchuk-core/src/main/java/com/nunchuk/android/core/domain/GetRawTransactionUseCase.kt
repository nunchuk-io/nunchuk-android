package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetRawTransactionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<GetRawTransactionUseCase.Param, String>(dispatcher) {
    override suspend fun execute(parameters: Param): String {
        return nunchukNativeSdk.getRawTransaction(walletId = parameters.walletId, txId = parameters.txId).orEmpty()
    }

    class Param(val walletId: String, val txId: String)
}