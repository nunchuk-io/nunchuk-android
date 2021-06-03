package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface CheckAddressValidUseCase {
    suspend fun execute(address: String): Result<Boolean>
}

internal class CheckAddressValidUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), CheckAddressValidUseCase {

    override suspend fun execute(address: String) = exe {
        nativeSdk.isValidAddress(address)
    }

}