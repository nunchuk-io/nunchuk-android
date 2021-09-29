package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetRemoteSignersUseCase {
    fun execute(): Flow<List<SingleSigner>>
}

internal class GetRemoteSignersUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetRemoteSignersUseCase {

    override fun execute() = flow {
        emit(nativeSdk.getRemoteSigners())
    }.catch { emit(emptyList()) }

}