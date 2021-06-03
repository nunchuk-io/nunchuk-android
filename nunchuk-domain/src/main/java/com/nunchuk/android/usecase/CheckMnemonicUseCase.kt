package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface CheckMnemonicUseCase {
    suspend fun execute(mnemonic: String): Result<Boolean>
}

internal class CheckMnemonicUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), CheckMnemonicUseCase {

    override suspend fun execute(mnemonic: String) = exe { nativeSdk.checkMnemonic(mnemonic) }

}