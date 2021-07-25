package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetMasterSignersUseCase {
    suspend fun execute(): Flow<List<MasterSigner>>
}

internal class GetMasterSignersUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetMasterSignersUseCase {

    override suspend fun execute() = flow { emit(nativeSdk.getMasterSigners()) }

}