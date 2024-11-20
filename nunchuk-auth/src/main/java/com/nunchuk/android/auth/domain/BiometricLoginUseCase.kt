package com.nunchuk.android.auth.domain

import com.nunchuk.android.auth.data.AuthRepository
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class BiometricLoginUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: AuthRepository,
    private val nativeSdk: NunchukNativeSdk,
    private val settingRepository: SettingRepository,
    private val storeAccountUseCase: StoreAccountUseCase
) : UseCase<Unit, AccountInfo?>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): AccountInfo? {
        val biometricConfig = settingRepository.biometricConfig.firstOrNull() ?: return null
        val (challengeId, challenge) = repository.biometricChallenge(biometricConfig.userId)
        val signature = nativeSdk.signLoginMessage(
            mnemonic = biometricConfig.privateKey,
            passphrase = "",
            message = challenge
        ).orEmpty()
        if (signature.isEmpty()) {
            throw IllegalStateException("Failed to sign challenge")
        }
        val userResponse = repository.biometricVerifyChallenge(challengeId = challengeId, signature)
        return storeAccountUseCase(
            StoreAccountUseCase.Param(
                "", userResponse,
                staySignedIn = true,
                fetchUserInfo = true
            )
        ).getOrNull()
    }
}