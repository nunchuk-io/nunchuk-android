package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.repository.TransactionRepository
import com.nunchuk.android.usecase.UpdateBiometricConfigUseCase
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class BiometricRegisterUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: AuthRepository,
    private val nativeSdk: NunchukNativeSdk,
    private val settingRepository: SettingRepository
) : UseCase<BiometricRegisterUseCase.Param, String>(ioDispatcher) {

    override suspend fun execute(parameters: Param): String {
        val privateKey = nativeSdk.generateMnemonic(12)
        val publicKey = nativeSdk.getPrimaryKeyAddress(privateKey, passphrase = "").orEmpty()
        if (publicKey.isEmpty()) {
            throw IllegalStateException("Failed to generate public key")
        }
        repository.biometricRegisterPublicKey(publicKey = publicKey, registerVerificationToken = parameters.registerVerificationToken)
        return privateKey
    }

    class Param(val registerVerificationToken: String)
}