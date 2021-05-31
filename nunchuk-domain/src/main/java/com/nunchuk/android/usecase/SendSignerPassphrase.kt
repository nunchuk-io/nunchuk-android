package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface SendSignerPassphrase {
    suspend fun execute(signerId: String, passphrase: String): Result<Unit>
}

internal class SendSignerPassphraseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), SendSignerPassphrase {

    override suspend fun execute(signerId: String, passphrase: String) = exe {
        nunchukFacade.sendSignerPassphrase(signerId, passphrase)
    }

}