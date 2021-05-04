package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface CreateSoftwareSignerUseCase {
    suspend fun execute(name: String, mnemonic: String, passphrase: String): Result<MasterSigner>
}

internal class CreateSoftwareSignerUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), CreateSoftwareSignerUseCase {

    override suspend fun execute(name: String, mnemonic: String, passphrase: String) = exe {
        nunchukFacade.createSoftwareSigner(
            name = name,
            mnemonic = mnemonic,
            passphrase = passphrase
        )
    }

}