package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetRemoteSignersUseCase {
    suspend fun execute(): Flow<List<SingleSigner>>
}

internal class GetRemoteSignersUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetRemoteSignersUseCase {

    override suspend fun execute() = flow { emit(nativeSdk.getRemoteSigners()) }

}