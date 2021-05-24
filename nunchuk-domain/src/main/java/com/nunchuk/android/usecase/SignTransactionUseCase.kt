package com.nunchuk.android.usecase

import com.nunchuk.android.model.Device
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface SignTransactionUseCase {
    suspend fun execute(walletId: String, txId: String, device: Device): Result<Unit>
}

internal class SignTransactionUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), SignTransactionUseCase {

    override suspend fun execute(walletId: String, txId: String, device: Device) = exe {
        nunchukFacade.signTransaction(
            walletId = walletId,
            txId = txId,
            device = device
        )
    }

}