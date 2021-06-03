package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface NewAddressUseCase {
    suspend fun execute(walletId: String, internal: Boolean = false): Result<String>
}

internal class NewAddressUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), NewAddressUseCase {

    override suspend fun execute(walletId: String, internal: Boolean) = exe {
        nativeSdk.newAddress(walletId = walletId, internal = internal)
    }

}