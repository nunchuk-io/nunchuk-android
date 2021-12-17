package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetAddressesUseCase {
    fun execute(
        walletId: String,
        used: Boolean = false,
        internal: Boolean = false
    ): Flow<List<String>>
}

internal class GetAddressesUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetAddressesUseCase {

    override fun execute(
        walletId: String,
        used: Boolean,
        internal: Boolean
    ) = flow {
        emit(nativeSdk.getAddresses(walletId = walletId, used = used, internal = internal))
    }

}