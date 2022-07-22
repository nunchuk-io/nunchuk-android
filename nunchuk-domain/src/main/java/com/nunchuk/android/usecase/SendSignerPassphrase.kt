package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface SendSignerPassphrase {
    fun execute(signerId: String, passphrase: String): Flow<Unit>
}

internal class SendSignerPassphraseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : SendSignerPassphrase {

    override fun execute(signerId: String, passphrase: String): Flow<Unit> = flow {
        emit(nativeSdk.sendSignerPassphrase(signerId, passphrase))
    }
}
