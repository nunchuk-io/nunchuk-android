package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetMasterSignerUseCase {
    suspend fun execute(masterSignerId: String): Result<MasterSigner>
}

internal class GetMasterSignerUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetMasterSignerUseCase {

    override suspend fun execute(masterSignerId: String) = exe { nunchukFacade.getMasterSigner(masterSignerId) }

}