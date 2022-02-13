package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CreatePassportSignersUseCase {
    fun execute(qrData: List<String>): Flow<List<SingleSigner>>
}

internal class CreatePassportSignersUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CreatePassportSignersUseCase {

    override fun execute(qrData: List<String>): Flow<List<SingleSigner>> = flow {
        emit(nativeSdk.parsePassportSigners(qrData))
    }

}