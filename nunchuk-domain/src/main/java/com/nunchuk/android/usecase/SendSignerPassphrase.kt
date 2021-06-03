package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface SendSignerPassphrase {
    suspend fun execute(signerId: String, passphrase: String): Result<Unit>
}

internal class SendSignerPassphraseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), SendSignerPassphrase {

    override suspend fun execute(signerId: String, passphrase: String) = exe {
        nativeSdk.sendSignerPassphrase(signerId, passphrase)
    }

}