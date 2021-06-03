package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetAddressBalanceUseCase {
    suspend fun execute(walletId: String, address: String): Result<Amount>
}

internal class GetAddressBalanceUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetAddressBalanceUseCase {

    override suspend fun execute(walletId: String, address: String) = exe {
        nativeSdk.getAddressBalance(walletId = walletId, address = address)
    }

}