package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CreateKeystoneSignerUseCase {
    fun execute(qrData: String): Flow<SingleSigner>
}

internal class CreateKeystoneSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CreateKeystoneSignerUseCase {

    override fun execute(qrData: String) = flow {
        emit(nativeSdk.createKeystoneSigner(qrData = qrData))
    }

}