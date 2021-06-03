package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface CreateSoftwareSignerUseCase {
    suspend fun execute(name: String, mnemonic: String, passphrase: String): Result<MasterSigner>
}

internal class CreateSoftwareSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), CreateSoftwareSignerUseCase {

    override suspend fun execute(name: String, mnemonic: String, passphrase: String) = exe {
        nativeSdk.createSoftwareSigner(
            name = name,
            mnemonic = mnemonic,
            passphrase = passphrase
        )
    }

}