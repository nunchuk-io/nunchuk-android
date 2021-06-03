package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetRemoteSignerUseCase {
    suspend fun execute(id: String): Result<SingleSigner>
}

internal class GetRemoteSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetRemoteSignerUseCase {

    override suspend fun execute(id: String) = exe { nativeSdk.getRemoteSigner(id) }

}