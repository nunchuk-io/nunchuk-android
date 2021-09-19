package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface NewAddressUseCase {
    suspend fun execute(walletId: String, internal: Boolean = false): Flow<String>
}

internal class NewAddressUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : NewAddressUseCase {

    override suspend fun execute(
        walletId: String,
        internal: Boolean
    ) = flow {
        try {
            emit(nativeSdk.newAddress(walletId = walletId, internal = internal))
        } catch (t: Throwable) {
            emit("")
        }
    }.catch { emit("") }

}