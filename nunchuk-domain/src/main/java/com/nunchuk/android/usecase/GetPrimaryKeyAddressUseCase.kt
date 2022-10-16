package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetPrimaryKeyAddressUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
): UseCase<GetPrimaryKeyAddressUseCase.Param, String?>(dispatcher) {
    override suspend fun execute(parameters: Param): String? {
        return nunchukNativeSdk.getPrimaryKeyAddress(parameters.mnemonic, parameters.passphrase)
    }

    data class Param(val mnemonic: String, val passphrase: String)
}