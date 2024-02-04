package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetUnusedWalletAddressUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<String, List<String>>(ioDispatcher){
    override suspend fun execute(parameters: String): List<String> {
        val addresses = nativeSdk.getAddresses(walletId = parameters, used = false, internal = false)
        if (addresses.isEmpty()) {
            return listOf(nativeSdk.newAddress(walletId = parameters, internal = false))
        }
        return nativeSdk.getAddresses(walletId = parameters, used = false, internal = false)
    }
}