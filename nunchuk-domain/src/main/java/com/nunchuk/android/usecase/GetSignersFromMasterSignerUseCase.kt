package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetSignersFromMasterSignerUseCase {
    suspend fun execute(masterSignerId: String): Result<List<SingleSigner>>
}

internal class GetSignersFromMasterSignerUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetSignersFromMasterSignerUseCase {

    override suspend fun execute(masterSignerId: String) = exe { nunchukFacade.getSignersFromMasterSigner(masterSignerId) }

}