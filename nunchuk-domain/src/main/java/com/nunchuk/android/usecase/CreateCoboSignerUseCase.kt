package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface CreateCoboSignerUseCase {
    suspend fun execute(name: String, jsonInfo: String): Result<SingleSigner>
}

internal class CreateCoboSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), CreateCoboSignerUseCase {

    override suspend fun execute(name: String, jsonInfo: String) = exe {
        nativeSdk.createCoboSigner(name, jsonInfo)
    }

}