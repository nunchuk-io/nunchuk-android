package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetMasterSignersUseCase {
    suspend fun execute(): Result<List<MasterSigner>>
}

internal class GetMasterSignersUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetMasterSignersUseCase {

    override suspend fun execute() = exe { nunchukFacade.getMasterSigners() }

}