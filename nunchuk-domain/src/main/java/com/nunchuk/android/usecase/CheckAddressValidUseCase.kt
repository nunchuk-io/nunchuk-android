package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface CheckAddressValidUseCase {
    suspend fun execute(address: String): Result<Boolean>
}

internal class CheckAddressValidUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), CheckAddressValidUseCase {

    override suspend fun execute(address: String) = exe {
        nunchukFacade.isValidAddress(address)
    }

}