package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetRemoteSignerUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<GetRemoteSignerUseCase.Data, SingleSigner>(dispatcher) {

    class Data(val id: String, val derivationPath: String)

    override suspend fun execute(parameters: Data): SingleSigner {
        return nativeSdk.getRemoteSigner(parameters.id, parameters.derivationPath)
    }
}