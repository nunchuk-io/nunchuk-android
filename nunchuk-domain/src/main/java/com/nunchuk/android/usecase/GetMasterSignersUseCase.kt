package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface GetMasterSignersUseCase {
    fun execute(): Flow<List<MasterSigner>>
}

internal class GetMasterSignersUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetMasterSignersUseCase {

    override fun execute() = flow {
        emit(nativeSdk.getMasterSigners())
    }.catch { emit(emptyList()) }.flowOn(Dispatchers.IO)

}