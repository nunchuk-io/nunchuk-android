package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetMnemonicCodeUseCase {
    suspend fun execute(): Result<String>
}

internal class GetMnemonicCodeUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GetMnemonicCodeUseCase {

    override suspend fun execute() = exe { nunchukFacade.generateMnemonicCode() }

}