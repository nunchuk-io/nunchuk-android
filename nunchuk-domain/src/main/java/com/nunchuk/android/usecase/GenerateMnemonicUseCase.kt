package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GenerateMnemonicUseCase {
    suspend fun execute(): Result<String>
}

internal class GenerateMnemonicUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GenerateMnemonicUseCase {

    override suspend fun execute() = exe { nativeSdk.generateMnemonic() }

}