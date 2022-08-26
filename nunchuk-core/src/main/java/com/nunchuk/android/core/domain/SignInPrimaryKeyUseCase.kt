package com.nunchuk.android.core.domain

import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.PrimaryKeyInfo
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignInPrimaryKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val signerSoftwareRepository: SignerSoftwareRepository,
    private val accountManager: AccountManager,
) : UseCase<SignInPrimaryKeyUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {

        val resultGetNonce = signerSoftwareRepository.getPKeyNonce(
            address = parameters.address,
            username = parameters.username
        )
        if (resultGetNonce.isBlank()) throw IllegalStateException()

        if (parameters.passphrase.isNotEmpty()) {
            nunchukNativeSdk.sendSignerPassphrase(
                parameters.masterFingerprint,
                parameters.passphrase
            )
        }

        val resultSignLoginMessage = nunchukNativeSdk.signLoginMessageImpl(
            parameters.masterFingerprint,
            "${parameters.username}${resultGetNonce}"
        )

        if (resultSignLoginMessage.isNullOrBlank()) throw IllegalStateException()

        val response = signerSoftwareRepository.pKeySignIn(
            address = parameters.address,
            username = parameters.username,
            signature = resultSignLoginMessage
        )

        accountManager.storeAccount(
            AccountInfo(
                token = response.tokenId,
                activated = true,
                staySignedIn = parameters.staySignedIn,
                name = parameters.username,
                username = parameters.username,
                deviceId = response.deviceId,
                loginType = SignInMode.PRIMARY_KEY.value,
                primaryKeyInfo = PrimaryKeyInfo(xfp = parameters.masterFingerprint)
            )
        )
    }

    data class Param(
        val passphrase: String,
        val address: String,
        val username: String,
        val signerName: String,
        val masterFingerprint: String,
        val staySignedIn: Boolean
    )
}