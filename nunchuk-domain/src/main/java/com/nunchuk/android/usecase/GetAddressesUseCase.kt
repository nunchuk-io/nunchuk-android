package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetAddressesUseCase {
    suspend fun execute(
        walletId: String,
        used: Boolean = false,
        internal: Boolean = false
    ): Result<List<String>>
}

internal class GetAddressesUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetAddressesUseCase {

    override suspend fun execute(walletId: String, used: Boolean, internal: Boolean) = exe {
        nunchukFacade.getAddresses(
            walletId = walletId,
            used = used,
            internal = internal
        )
    }

}