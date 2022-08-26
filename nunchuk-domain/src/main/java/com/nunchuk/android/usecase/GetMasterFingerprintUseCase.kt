package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetMasterFingerprintUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<GetMasterFingerprintUseCase.Param, String?>(dispatcher) {

    override suspend fun execute(parameters: Param): String? {
        return nunchukNativeSdk.getMasterFingerprint(parameters.mnemonic, parameters.passphrase)
    }

    data class Param(val mnemonic: String, val passphrase: String)
}