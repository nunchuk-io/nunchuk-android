package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetAddressesUseCase {
    suspend fun execute(
        walletId: String,
        used: Boolean = false,
        internal: Boolean = false
    ): Result<List<String>>
}

internal class GetAddressesUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetAddressesUseCase {

    override suspend fun execute(walletId: String, used: Boolean, internal: Boolean) = exe {
        nativeSdk.getAddresses(
            walletId = walletId,
            used = used,
            internal = internal
        )
    }

}