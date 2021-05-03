package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GenerateMnemonicUseCase {
    suspend fun execute(): Result<String>
}

internal class GenerateMnemonicUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), GenerateMnemonicUseCase {

    override suspend fun execute() = exe { nunchukFacade.generateMnemonic() }

}