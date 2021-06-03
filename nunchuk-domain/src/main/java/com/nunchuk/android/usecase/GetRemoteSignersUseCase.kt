package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetRemoteSignersUseCase {
    suspend fun execute(): Result<List<SingleSigner>>
}

internal class GetRemoteSignersUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetRemoteSignersUseCase {

    override suspend fun execute() = exe { nativeSdk.getRemoteSigners() }

}