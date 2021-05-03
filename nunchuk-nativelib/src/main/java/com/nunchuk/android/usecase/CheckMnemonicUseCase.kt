package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface CheckMnemonicUseCase {
    suspend fun execute(mnemonic: String): Result<Boolean>
}

internal class CheckMnemonicUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), CheckMnemonicUseCase {

    override suspend fun execute(mnemonic: String) = exe { nunchukFacade.checkMnemonic(mnemonic) }

}